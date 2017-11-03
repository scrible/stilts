package org.projectodd.stilts.stomp.server.protocol.http;

import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;
import org.projectodd.stilts.stomp.spi.StompConnection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {

    private Map<String, ConnectionContext> connections = Collections.synchronizedMap(new HashMap<String, ConnectionContext>());
    
    public ConnectionManager() {
        
    }
    
    public ConnectionContext get(String connectionId) {
        return this.connections.get(  connectionId  );
    }
    
    public void put(String connectionId, ConnectionContext connectionContext) {
        this.connections.put( connectionId, connectionContext );
    }

    public void remove(String connectionId) {
        this.connections.remove(connectionId);
    }

    public void removeConnection(StompConnection conn) {
        synchronized (this.connections) {
            for (String key : this.connections.keySet()) {
                ConnectionContext ctx = this.connections.get(key);
                if (ctx.getStompConnection() == conn) {
                    this.connections.remove(key);
                    break;
                }
            }
        }
    }
}
