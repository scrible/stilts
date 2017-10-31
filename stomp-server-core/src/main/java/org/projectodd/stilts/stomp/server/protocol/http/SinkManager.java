package org.projectodd.stilts.stomp.server.protocol.http;

import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;

import java.util.*;

public class SinkManager {

    public SinkManager() {

    }

    public HttpMessageSink get(ConnectionContext connection) {
        return this.sinks.get( connection );
    }

    public void put(ConnectionContext connection, HttpMessageSink sink) {
        this.sinks.put( connection, sink );
    }

    public void remove(ConnectionContext connection) {
        this.sinks.remove(connection);
    }

    public Set<Map.Entry<ConnectionContext, HttpMessageSink>> list() {
        Set<Map.Entry<ConnectionContext, HttpMessageSink>> entries = null;
        //get copy of the entries from the map in a synchronized block
        synchronized (sinks) {
            entries = new HashSet<Map.Entry<ConnectionContext, HttpMessageSink>>(sinks.entrySet());
        }
        return entries;
    }

    private Map<ConnectionContext, HttpMessageSink> sinks = Collections.synchronizedMap(new HashMap<ConnectionContext, HttpMessageSink>());

}
