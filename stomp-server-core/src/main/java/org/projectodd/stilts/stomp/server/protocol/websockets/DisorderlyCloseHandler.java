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
            	String exceptionMessage = cause.getMessage();
                ctx.setAttachment( Boolean.TRUE );
                ctx.sendUpstream( new DisorderlyCloseEvent( ctx.getChannel() ) );
                ctx.getChannel().disconnect();
            	if (exceptionMessage != null && exceptionMessage.contains("reset by peer")) {
            		log.info(ctx.getChannel() + " connection reset by peer. disconnect channel and send "
            				+ "DisorderlyCloseEvent upstream.");
            	} else {
                	log.warn(ctx.getChannel() + "disconnect channel and send DisorderlyCloseEvent upstream due to "
                			+ "IOException: ", cause);            		
            	}
            }
        } else {
            ctx.sendUpstream( e );
        }
    }

}
