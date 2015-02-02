package org.projectodd.stilts.stomp.server.protocol;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.projectodd.stilts.stomp.server.protocol.websockets.DisorderlyCloseEvent;
import org.projectodd.stilts.stomp.spi.StompConnection;
import org.projectodd.stilts.stomp.spi.StompProvider;

public class StompDisorderlyCloseHandler extends AbstractProviderHandler {

    private static Logger log = Logger.getLogger( StompDisorderlyCloseHandler.class );

    public StompDisorderlyCloseHandler(StompProvider provider, ConnectionContext context) {
        super( provider, context );
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof DisorderlyCloseEvent) {
            log.warn("handle disorderly closed channel: " + e.getChannel());
            StompConnection stompConnection = getStompConnection();
            if (stompConnection != null) {
                stompConnection.disconnect();
            }
            getContext().setActive(false);
            ctx.getChannel().close();
        } else {
            super.handleUpstream(ctx, e);
        }
    }

}
