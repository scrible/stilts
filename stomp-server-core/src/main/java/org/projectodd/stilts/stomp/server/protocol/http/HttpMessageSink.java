package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.Channel;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.TransactionalAcknowledger;
import org.projectodd.stilts.stomp.server.protocol.AckManager;
import org.projectodd.stilts.stomp.spi.TransactionalAcknowledgeableMessageSink;

import java.util.LinkedList;

public class HttpMessageSink implements TransactionalAcknowledgeableMessageSink {

    public HttpMessageSink(AckManager ackManager) {
        this.ackManager = ackManager;
    }

    @Override
    public void send(StompMessage message) throws StompException {
        send( message, null );
    }

    @Override
    public synchronized void send(StompMessage message, TransactionalAcknowledger acknowledger) throws StompException {
        log.debug( "someone sent a message: " + message );
        if (acknowledger != null) {
            this.ackManager.registerAcknowledger( message.getId(), acknowledger );
        }

        if (this.channel != null) {
            log.debug( "write message to channel : " + message );
            this.channel.write( message );
            if (this.single) {
                this.channel = null;
            }
        } else {
            synchronized (this.messages) {
                this.messages.add(message);
            }
        }
    }

    public void provideChannel(Channel channel, boolean single) {
        log.debug( "someone provided a channel: " + channel );

        synchronized (this.messages) {
            if (single && !this.messages.isEmpty()) {
                StompMessage message = messages.removeFirst();
                channel.write(message);
                return;
            }

            if (!single) {
                for (StompMessage each : this.messages) {
                    channel.write(each);
                }
                this.messages.clear();
            }
        }

        synchronized (this) {
            this.channel = channel;
            this.single = single;
        }
        //ensure no messages were queued while we were specifying the channel or else they won't get sent
        synchronized (this.messages) {
            if (!single) {
                for (StompMessage each : this.messages) {
                    channel.write(each);
                }
                this.messages.clear();
            }
        }
    }

    public synchronized void clearChannel() {
        this.channel = null;
        this.single = false;
    }

    private static Logger log = Logger.getLogger( HttpMessageSink.class );

    protected AckManager ackManager;
    protected Channel channel;
    protected LinkedList<StompMessage> messages = new LinkedList<StompMessage>();
    protected boolean single;

}
