package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.channel.socket.nio.NioSocketChannelWriteBufferInspector;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.TransactionalAcknowledger;
import org.projectodd.stilts.stomp.server.protocol.AckManager;
import org.projectodd.stilts.stomp.spi.TransactionalAcknowledgeableMessageSink;

import java.util.Date;
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
    public synchronized void send(final StompMessage message, TransactionalAcknowledger acknowledger) throws StompException {
        log.debug( "someone sent a message: " + message );
        //System.out.println( "someone sent a message: " + message );
        if (acknowledger != null) {
            this.ackManager.registerAcknowledger( message.getId(), acknowledger );
        }

        if (this.messages == null) {
            //This sink has been marked abandoned.  The lack of response to a ping/pong should hopefully cause client to
            //reconnect from scratch.
            //Stop here so even if we somehow get a channel, we don't send back messages.  This will prevent a client from
            //reconnecting to an old sink and miss messages.
            return;
        }

        if (this.channel != null) {
            if (this.channel instanceof NioSocketChannel) {
                //HACK!!!! in production have situations where these are left dangling which leads to problems of running out of memory.
                NioSocketChannel nsc = (NioSocketChannel) this.channel;
                int pendingSize = new NioSocketChannelWriteBufferInspector(nsc).getWriteBufferPendingSize();
                if (pendingSize > 5000000) {
                    log.warn("nioSocketChannel - outgoing write buffer more than 5MB! - removing: pending write size=" + pendingSize);
                    //looks like a dead connection!
                    nsc.close();
                    this.messages = null;
                    return;
                }
            }
            log.debug( "write message to channel : " + message );
            //System.out.println( "  write message to channel: " + message );
            final ChannelFuture cf = this.channel.write(message);
            this.lastMessageSent = new Date();
            if (this.single) {
                final Channel curChannel = channel;
                this.channel = null; //we're using this channel so clear it out.
                this.lastHadChannelTimestamp = new Date();
                cf.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (curChannel != null) curChannel.close();
                        if (!cf.isSuccess()) {
                            //this message failed to send
                            log.error("Failed to send message (send): " + message);
                        }
                    }
                });
            }
        } else {
            if (this.lastMessageSent == null
                    || this.lastMessageSent.before(new Date(new Date().getTime() - 60 * 1000)) && !this.messages.isEmpty()) {
                //if haven't sent a message in a minute (and have pending messages), this message sink is abandoned, clear out the pending messages.
                log.warn("Marking sink as abandoned - no messages sent in last minute- has " + this.messages.size() + " pending messages");
                this.messages.clear();
                this.messages = null;
            }
            if (this.messages == null) {
                //This sink has been marked abandoned.  The lack of response to a ping/pong should hopefully
                return;
            }
            //System.out.println( "  Queue message: " + message );
            synchronized (this.messages) {
                this.messages.add(message);
            }
        }
    }

    public void provideChannel(final Channel channel, boolean single) {
        log.debug( "someone provided a channel: " + channel );
        //System.out.println( "  someone provided a channel: " + channel );
        synchronized (this.messages) {
            if (single && !this.messages.isEmpty()) {
                final StompMessage message = messages.removeFirst();
                //System.out.println( "  Pending message to be sent: " + message );
                final ChannelFuture cf = channel.write(message);
                this.lastMessageSent = new Date();
                final Channel curChannel = channel;
                cf.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        //System.out.println( "  Sent message complete: " + message );
                        if (curChannel != null) curChannel.close();
                        if (!cf.isSuccess()) {
                            //this message failed to send
                            log.error("Failed to send message (provide channel): " + message);
                        }
                    }
                });
                channel.close();
                this.lastHadChannelTimestamp = new Date();
                return;
            }

            if (!single) {
                for (StompMessage each : this.messages) {
                    channel.write(each);
                }
                this.lastMessageSent = new Date();
                this.messages.clear();
            }
        }

        synchronized (this) {
            if (this.channel != null) {
                try {
                    this.channel.close();
                } catch (Exception e) {
                    log.info("Failed to close old provided channel", e);
                }
            }
            this.lastHadChannelTimestamp = single ? new Date() : null;
            this.channel = channel;
            this.single = single;
        }
        //ensure no messages were queued while we were specifying the channel for SSE or else they won't get sent
        synchronized (this.messages) {
            if (!single) {
                for (StompMessage each : this.messages) {
                    channel.write(each);
                }
                this.lastMessageSent = new Date();
                this.messages.clear();
            }
        }
    }

    public synchronized void clearChannel() {
        if (this.channel != null) {
            try {
                this.channel.close();
            } catch (Exception e) {
                log.warn("Failed to close channel in HttpMessageSink.clearChannel", e);
            }
        }
        this.channel = null;
        this.single = false;
        this.lastHadChannelTimestamp = new Date();
    }

    public Date getLastHadChannelTimestamp() {
        return this.lastHadChannelTimestamp;
    }

    private static Logger log = Logger.getLogger( HttpMessageSink.class );

    protected AckManager ackManager;
    protected Channel channel;
    protected LinkedList<StompMessage> messages = new LinkedList<StompMessage>();
    protected boolean single;
    protected Date lastHadChannelTimestamp = new Date();
    protected Date lastMessageSent = new Date();
    protected Date sinkCreationDate = lastMessageSent;

    public boolean isSingle() {
        return this.single;
    }

    public Date getLastMessageSent() {
        return lastMessageSent;
    }

    public Date getSinkCreationDate() {
        return sinkCreationDate;
    }

    public boolean isAbandoned() {
        return this.messages == null;
    }
}
