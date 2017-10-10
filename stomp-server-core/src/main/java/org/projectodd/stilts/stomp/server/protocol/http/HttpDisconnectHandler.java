package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.projectodd.stilts.stomp.protocol.StompFrame;
import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;
import org.projectodd.stilts.stomp.server.protocol.DisconnectHandler;
import org.projectodd.stilts.stomp.spi.StompProvider;

public class HttpDisconnectHandler extends DisconnectHandler {
    private static Logger log = Logger.getLogger(DisconnectHandler.class);

    public HttpDisconnectHandler(StompProvider server, ConnectionContext context) {
        super(server, context);
    }

    public HttpDisconnectHandler(StompProvider server, ConnectionContext context, boolean shouldClose) {
        super(server, context, shouldClose);
    }

    @Override
    public void handleControlFrame(ChannelHandlerContext channelContext, StompFrame frame) {
        try {
            HttpResponse httpResp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            httpResp.setHeader("Content-Length", 0);
            httpResp.setHeader("Content-Type", "text/stomp");
            ChannelFuture future = Channels.future(channelContext.getChannel());
            channelContext.sendDownstream(new DownstreamMessageEvent(channelContext.getChannel(), future, httpResp, channelContext.getChannel().getRemoteAddress()));
            channelContext.getChannel().close();
        } catch (Exception e) {
        }

        super.handleControlFrame(channelContext, frame);
    }
}
