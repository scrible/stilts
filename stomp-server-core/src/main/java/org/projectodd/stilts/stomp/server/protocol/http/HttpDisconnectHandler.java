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
import org.projectodd.stilts.stomp.server.protocol.WrappedConnectionContext;
import org.projectodd.stilts.stomp.spi.StompProvider;

public class HttpDisconnectHandler extends DisconnectHandler {
    private static Logger log = Logger.getLogger(DisconnectHandler.class);
    protected SinkManager sinkManager;

    public HttpDisconnectHandler(StompProvider server, ConnectionContext context, ConnectionManager connectionManager, SinkManager sinkManager) {
        super(server, context, connectionManager);
        this.sinkManager = sinkManager;
    }

    public HttpDisconnectHandler(StompProvider server, ConnectionContext context, ConnectionManager connectionManager, SinkManager sinkManager, boolean shouldClose) {
        super(server, context, connectionManager, shouldClose);
        this.sinkManager = sinkManager;
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
        ConnectionContext ctx = this.getContext();
        while (ctx instanceof WrappedConnectionContext) {
            this.sinkManager.remove(ctx);
            ctx = ((WrappedConnectionContext) ctx).getConnectionContext();
        }
        this.sinkManager.remove(ctx);


        super.handleControlFrame(channelContext, frame);
    }
}
