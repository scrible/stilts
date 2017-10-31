package org.projectodd.stilts.stomp.server.protocol.http;

import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;

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

}
