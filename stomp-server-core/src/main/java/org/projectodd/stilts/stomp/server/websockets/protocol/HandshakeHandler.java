/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.projectodd.stilts.stomp.server.websockets.protocol;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamChannelStateEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;
import org.projectodd.stilts.stomp.protocol.DebugHandler;

/**
 * Multi-verison handshake handler for the web-sockets protocol family.
 * 
 * @see Handshake
 * 
 * @author Trustin Lee
 * @author Michael Dobozy
 * @author Bob McWhirter
 */
public class HandshakeHandler extends SimpleChannelUpstreamHandler {

    /**
     * Construct.
     * 
     * @param contextRegistry The context registry.
     */
    public HandshakeHandler() {
        this.handshakes.add( new Handshake_Ietf00() );
        this.handshakes.add( new Handshake_Hixie75() );
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest( ctx, (HttpRequest) msg );
        } else {
            super.messageReceived( ctx, e );
        }
    }

    /**
     * Handle initial HTTP portion of the handshake.
     * 
     * @param channelContext
     * @param request
     * @throws Exception
     */
    protected void handleHttpRequest(final ChannelHandlerContext channelContext, HttpRequest request) throws Exception {
        if (isWebSocketsUpgradeRequest( request )) {

            Handshake handshake = findHandshake( request );

            if (handshake != null) {

                HttpResponse response = handshake.generateResponse( request );

                response.addHeader( Names.UPGRADE, Values.WEBSOCKET );
                response.addHeader( Names.CONNECTION, Values.UPGRADE );

                final ChannelPipeline pipeline = channelContext.getChannel().getPipeline();
                reconfigureUpstream( pipeline );

                // TODO FIXME
                // addContextHandler( channelContext, context, pipeline );

                Channel channel = channelContext.getChannel();
                ChannelFuture future = channel.write( response );
                future.addListener( new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) throws Exception {
                        reconfigureDownstream( pipeline );
                        pipeline.remove( HandshakeHandler.this );
                        forwardConnectEventUpstream( channelContext );
                    }
                } );

                return;
            }
        }

        // Send an error page otherwise.
        sendHttpResponse( channelContext, request, new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN ) );
    }

    protected void forwardConnectEventUpstream(ChannelHandlerContext channelContext) {
        ChannelEvent connectEvent = new UpstreamChannelStateEvent( channelContext.getChannel(), ChannelState.CONNECTED, channelContext.getChannel().getRemoteAddress() );
        channelContext.sendUpstream( connectEvent );
    }

    /**
     * Locate a matching handshake version.
     * 
     * @param request The HTTP request.
     * @return The matching handshake, otherwise <code>null</code> if none
     *         match.
     */
    protected Handshake findHandshake(HttpRequest request) {
        for (Handshake handshake : this.handshakes) {
            if (handshake.matches( request )) {
                return handshake;
            }
        }

        return null;
    }

    /**
     * Remove HTTP handlers, replace with web-socket handlers.
     * 
     * @param pipeline The pipeline to reconfigure.
     */
    protected void reconfigureUpstream(ChannelPipeline pipeline) {
        pipeline.replace( "http-decoder", "websockets-decoder", new WebSocketFrameDecoder() );
        pipeline.addAfter(  "websockets-decoder", "debug-websockets-TAIL", new DebugHandler( "websockets.SERVER-TAIL" ) );
    }

    /**
     * Remove HTTP handlers, replace with web-socket handlers
     * 
     * @param pipeline The pipeline to reconfigure.
     */
    protected void reconfigureDownstream(ChannelPipeline pipeline) {
        pipeline.replace( "http-encoder", "websockets-encoder", new WebSocketFrameEncoder() );
        pipeline.addBefore(  "websockets-encoder", "debug-websockets-HEAD", new DebugHandler( "websockets.SERVER-HEAD" ) );
    }

    /**
     * Determine if this request represents a web-socket upgrade request.
     * 
     * @param request The request to inspect.
     * @return <code>true</code> if this request is indeed a web-socket upgrade
     *         request, otherwise <code>false</code>.
     */
    protected boolean isWebSocketsUpgradeRequest(HttpRequest request) {
        return (Values.UPGRADE.equalsIgnoreCase( request.getHeader( Names.CONNECTION ) ) && Values.WEBSOCKET.equalsIgnoreCase( request.getHeader( Names.UPGRADE ) ));
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(
                    ChannelBuffers.copiedBuffer(
                            res.getStatus().toString(), CharsetUtil.UTF_8 ) );
            HttpHeaders.setContentLength( res, res.getContent().readableBytes() );
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.getChannel().write( res );
        if (!HttpHeaders.isKeepAlive( req ) || res.getStatus().getCode() != 200) {
            f.addListener( ChannelFutureListener.CLOSE );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    private static final Logger log = Logger.getLogger( "org.torquebox.web.websockets.protocol" );
    private List<Handshake> handshakes = new ArrayList<Handshake>();

}