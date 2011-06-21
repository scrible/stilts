package org.projectodd.stilts.clownshoes.server;

import org.projectodd.stilts.circus.MessageConduitFactory;
import org.projectodd.stilts.circus.server.StandaloneCircusServer;
import org.projectodd.stilts.circus.xa.XAMessageConduitFactory;
import org.projectodd.stilts.circus.xa.psuedo.PsuedoXAMessageConduitFactory;
import org.projectodd.stilts.clownshoes.stomplet.SimpleStompletContainer;
import org.projectodd.stilts.clownshoes.stomplet.StompletContainer;
import org.projectodd.stilts.clownshoes.stomplet.StompletMessageConduitFactory;

public class StandaloneStompletCircusServer extends StandaloneCircusServer<StompletCircusServer> {

    public StandaloneStompletCircusServer(StompletCircusServer server) {
        super( server );
    }

    public void configure() throws Throwable {
        super.configure();
        SimpleStompletContainer stompletContainer = new SimpleStompletContainer( );
        MessageConduitFactory conduitFactory = new StompletMessageConduitFactory( stompletContainer );
        XAMessageConduitFactory xaConduitFactory = new PsuedoXAMessageConduitFactory( conduitFactory );
        getServer().setStompletContainer( stompletContainer );
        getServer().setMessageConduitFactory( xaConduitFactory );
    }
    
    public StompletContainer getStompletContainer() {
        return getServer().getStompletContainer();
    }
    
    public void stop() throws Throwable {
        super.stop();
    }

}