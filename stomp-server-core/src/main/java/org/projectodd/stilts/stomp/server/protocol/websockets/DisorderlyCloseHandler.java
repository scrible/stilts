package org.projectodd.stilts.stomp.server.protocol.websockets;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class DisorderlyCloseHandler extends SimpleChannelUpstreamHandler {

    private static Logger log = Logger.getLogger( DisorderlyCloseHandler.class );

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable cause = e.getCause();

        if (cause instanceof IOException ) {
            if (ctx.getAttachment() == null) {
            	log.warn("disconnect channel and send DisorderlyCloseEvent upstream due to IOException: ", cause);
                ctx.setAttachment( Boolean.TRUE );
                ctx.sendUpstream( new DisorderlyCloseEvent( ctx.getChannel() ) );
                ctx.getChannel().disconnect();
            }
        } else {
            ctx.sendUpstream( e );
        }
    }

}
