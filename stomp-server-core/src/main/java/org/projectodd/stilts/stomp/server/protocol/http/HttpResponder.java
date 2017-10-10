/*
 * Copyright 2011 Red Hat, Inc, and individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.protocol.StompFrame;
import org.projectodd.stilts.stomp.protocol.StompFrame.Command;

public class HttpResponder implements ChannelUpstreamHandler {

    public HttpResponder() {

    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent && ((MessageEvent) e).getMessage() instanceof StompFrame) {
            final StompFrame frame = (StompFrame) ((MessageEvent) e).getMessage();
            if (frame.getCommand() != Command.CONNECT && frame.getCommand() != Command.DISCONNECT) {
                HttpResponse httpResp = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT );
                httpResp.setHeader( "Content-Length", "0" );
                ChannelFuture future = Channels.future(ctx.getChannel());
                ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, httpResp, ctx.getChannel().getRemoteAddress()));
                ctx.getChannel().close();
                return;
            }
        } else if (e instanceof MessageEvent && ((MessageEvent) e).getMessage() instanceof StompMessage) {
            //Send a response for messages
            HttpResponse httpResp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            httpResp.setHeader("Content-Length", "0");
            final StompMessage msg = (StompMessage) ((MessageEvent) e).getMessage();
            ChannelFuture future = Channels.future(ctx.getChannel());
            ctx.sendDownstream(new DownstreamMessageEvent(ctx.getChannel(), future, httpResp, ctx.getChannel().getRemoteAddress()));
            ctx.getChannel().close();
        }


        ctx.sendUpstream( e );
    }

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger( HttpResponder.class );

}
