package org.projectodd.stilts.stomp.server.protocol.http;

import org.jboss.logging.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.projectodd.stilts.stomp.server.protocol.ConnectionContext;
import org.projectodd.stilts.stomp.server.protocol.DefaultConnectionContext;
import org.projectodd.stilts.stomp.server.protocol.WrappedConnectionContext;
import org.projectodd.stilts.stomp.server.protocol.websockets.SessionDecodedEvent;

import java.net.URI;
import java.util.Random;
import java.util.Set;

public class ConnectionResumeHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {

    public ConnectionResumeHandler(ConnectionManager connectionManager, WrappedConnectionContext context) {
        this.connectionManager = connectionManager;
        this.context = context;
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            if (((MessageEvent) e).getMessage() instanceof HttpRequest) {

                CookieDecoder cookieDecoder = new CookieDecoder();
                ConnectionContext connectionContext = null;

                //Rely on an HTTP header maintain the connection ID
                HttpRequest httpReq = (HttpRequest) ((MessageEvent) e).getMessage();
                String connectionIdHeader = httpReq.getHeader("X-Stomp-Connection-Id");
                if (connectionIdHeader == null) {
                    try {
                        URI uri = new URI(httpReq.getUri());
                        String query = uri.getQuery();
                        if (query != null && query.length() > 0) {
                            String[] paramsKVs = query.split("&");
                            for (int i = 0; i < paramsKVs.length; i++) {
                                String paramsKV = paramsKVs[i];
                                if (paramsKV.startsWith("stompConnectionId=")) {
                                    connectionIdHeader = paramsKV.substring("stompConnectionId=".length());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        //ignore failures to parse this silently
                    }
                }
                if (connectionIdHeader != null) {
                    this.connectionId = connectionIdHeader;
                    connectionContext = this.connectionManager.get(connectionId);
                }

                String cookieHeader = httpReq.getHeader( "Cookie" );
/*
                if (cookieHeader != null) {
                    Set<Cookie> cookies = cookieDecoder.decode( cookieHeader );
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals( "stomp-connection-id" )) {
                            this.connectionId = cookie.getValue();
                            connectionContext = this.connectionManager.get( connectionId );
                            break;
                        }
                    }
                }
*/

                if (connectionContext == null) {
                    connectionContext = new DefaultConnectionContext();
                    this.connectionId = createConnectionId( connectionContext );
                    this.connectionManager.put( connectionId, connectionContext );
                }

                this.context.setConnectionContext( connectionContext );

                if (cookieHeader != null) {
                    Set<Cookie> cookies = cookieDecoder.decode( cookieHeader );

                    for (Cookie each : cookies) {
                        if (each.getName().equalsIgnoreCase( "jsessionid" )) {
                            ChannelEvent sessionDecodedEvent = new SessionDecodedEvent( ctx.getChannel(), each.getValue() );
                            ctx.sendUpstream( sessionDecodedEvent );
                            break;
                        }
                    }
                }
                // }
            }
        }
        ctx.sendUpstream( e );
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            if (((MessageEvent) e).getMessage() instanceof HttpResponse) {
                HttpResponse httpResp = (HttpResponse) ((MessageEvent) e).getMessage();
                httpResp.setHeader("X-Stomp-Connection-Id", this.connectionId);

/*
                CookieEncoder cookieEncoder = new CookieEncoder( true );
                String cookieHeader = httpResp.getHeader( "Set-Cookie" );
                if (cookieHeader != null) {
                    CookieDecoder cookieDecoder = new CookieDecoder();
                    Set<Cookie> cookies = cookieDecoder.decode( cookieHeader );

                    for (Cookie each : cookies) {
                        cookieEncoder.addCookie( each );
                    }
                }
                Cookie connectionCookie = new DefaultCookie( "stomp-connection-id", this.connectionId );
                cookieEncoder.addCookie( connectionCookie );
                httpResp.setHeader( "Set-Cookie", cookieEncoder.encode() );
*/
            }
        }

        ctx.sendDownstream( e );
    }

    public static String createConnectionId(ConnectionContext connectionContext) {
        return Long.toHexString( new Random( System.identityHashCode( connectionContext ) ).nextLong() );
    }

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger( ConnectionResumeHandler.class );

    private ConnectionManager connectionManager;
    private WrappedConnectionContext context;
    private String connectionId;

}
