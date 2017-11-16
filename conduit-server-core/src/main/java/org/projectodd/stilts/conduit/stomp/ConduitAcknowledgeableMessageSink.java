package org.projectodd.stilts.conduit.stomp;

import org.projectodd.stilts.stomp.Acknowledger;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.spi.AcknowledgeableMessageSink;
import org.projectodd.stilts.stomp.spi.TransactionalAcknowledgeableMessageSink;

public class ConduitAcknowledgeableMessageSink implements AcknowledgeableMessageSink {

    ConduitAcknowledgeableMessageSink(final TransactionalAcknowledgeableMessageSink sink) {
        this.sink = sink;
    }

    void setConnection(final ConduitStompConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(final StompMessage message) throws StompException {
        send( message, null );
    }

    @Override
    public void send(final StompMessage message, Acknowledger acknowledger) throws StompException {
        if (acknowledger != null) {
            ConduitAcknowledger conduitAcknowledger = new ConduitAcknowledger( this.connection, acknowledger );
            this.sink.send( message, conduitAcknowledger );
        } else {
            this.sink.send( message );
        }
    }

    public TransactionalAcknowledgeableMessageSink getSink() {
        return this.sink;
    }

    private final TransactionalAcknowledgeableMessageSink sink;
    private ConduitStompConnection connection;

}
