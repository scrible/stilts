package org.projectodd.stilts.stomp.server.protocol.websockets;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class DisorderlyCloseHandler extends SimpleChannelUpstreamHandler {

    private static Logger log = Logger.getLogger( DisorderlyCloseHandler.class );

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable cause = e.getCause();

        if (cause instanceof IOException) {
            if (ctx.getAttachment() == null) {
                String exceptionMessage = cause.getMessage();
                ctx.setAttachment(Boolean.TRUE);
                if (cause instanceof ClosedChannelException) {
                    log.info(ctx.getChannel() + "channel already closed. send DisorderlyCloseEvent upstream.");
                } else {
                    if (exceptionMessage != null && exceptionMessage.contains("reset by peer")) {
                        log.info(ctx.getChannel() + " connection reset by peer. close channel and send "
                                + "DisorderlyCloseEvent upstream.");
                    } else if (exceptionMessage != null && exceptionMessage.contains("Connection timed out")) {
                        log.info(ctx.getChannel() + " connection timed out. close channel and send "
                                + "DisorderlyCloseEvent upstream.");
                    } else {
                        log.warn(ctx.getChannel() + "close channel and send DisorderlyCloseEvent upstream due to "
                                + "IOException: ", cause);
                    }
                }
                Channel channel = ctx.getChannel();
                ctx.sendUpstream(new DisorderlyCloseEvent(channel));
                channel.close();
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

}
