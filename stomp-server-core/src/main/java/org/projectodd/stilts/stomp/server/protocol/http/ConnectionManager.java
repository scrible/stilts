package org.projectodd.stilts.stomp.server.protocol.http;

import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;
import org.projectodd.stilts.stomp.server.protocol.WrappedConnectionContext;
import org.projectodd.stilts.stomp.spi.StompConnection;

import java.util.*;

public class ConnectionManager {

    private Map<String, ConnectionContext> connections = Collections.synchronizedMap(new HashMap<String, ConnectionContext>());
    
    public ConnectionManager() {
        
    }
    
    public ConnectionContext get(String connectionId) {
        return this.connections.get(  connectionId  );
    }

    public int getConnectionCount() {
        return this.connections.size();
    }
    
    public void put(String connectionId, ConnectionContext connectionContext) {
        this.connections.put( connectionId, connectionContext );
    }

    public void remove(String connectionId) {
        this.connections.remove(connectionId);
    }

    public void remove(ConnectionContext connectionCtx) {
        while (connectionCtx instanceof WrappedConnectionContext) {
            ConnectionContext wrappedConnectionCtx = ((WrappedConnectionContext) connectionCtx).getConnectionContext();
            if (wrappedConnectionCtx == connectionCtx) break;
            connectionCtx = wrappedConnectionCtx;
        }
        this.connections.remove(ConnectionResumeHandler.createConnectionId(connectionCtx));
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

    public Set<Map.Entry<String, ConnectionContext>> list() {
        Set<Map.Entry<String, ConnectionContext>> entries = null;
        synchronized (this.connections) {
            entries = new HashSet<Map.Entry<String, ConnectionContext>>(this.connections.entrySet());
        }
        return entries;
    }
}
