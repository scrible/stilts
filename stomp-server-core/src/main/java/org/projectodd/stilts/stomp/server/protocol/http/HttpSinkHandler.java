package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.projectodd.stilts.stomp.server.protocol.WrappedConnectionContext;

public class HttpSinkHandler extends SimpleChannelUpstreamHandler {

    public HttpSinkHandler(WrappedConnectionContext context, SinkManager sinkManager) {
        this.context = context;
        this.sinkManager = sinkManager;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest httpReq = (HttpRequest) e.getMessage();
            if (httpReq.getMethod().equals( HttpMethod.GET ) && "text/stomp-poll".equals( httpReq.getHeader( "Accept" ) )) {
                log.debug( "Hooking up the sink" );
                HttpMessageSink sink = this.sinkManager.get( this.context.getConnectionContext() );
                if (sink != null) {
                    sink.provideChannel(ctx.getChannel(), true);
                    this.provided = true;
                } else {
                    HttpResponse httpResp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                    httpResp.setHeader("Content-Length", "0");
                    ChannelFuture future = Channels.future(ctx.getChannel());
                    ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, httpResp, ctx.getChannel().getRemoteAddress()));
                    ctx.getChannel().close();
                    return;
                }
            }
        }
        if (!this.provided) {
            log.debugf( "NOT Hooking up the sink %s", e );
        }
        super.messageReceived( ctx, e );
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (this.provided) {
            HttpMessageSink sink = this.sinkManager.get( this.context.getConnectionContext() );
            if (sink != null) sink.clearChannel();
            this.sinkManager.remove(this.context.getConnectionContext());
        }
    }

    private static Logger log = Logger.getLogger( HttpSinkHandler.class );

    private boolean provided = false;
    private WrappedConnectionContext context;
    private SinkManager sinkManager;

}
