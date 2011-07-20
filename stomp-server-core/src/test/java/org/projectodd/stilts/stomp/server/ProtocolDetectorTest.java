package org.projectodd.stilts.stomp.server;

import static org.junit.Assert.*;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.junit.Before;
import org.junit.Test;
import org.projectodd.stilts.stomp.protocol.PipelineExposer;
import org.projectodd.stilts.stomp.protocol.StompFrameDecoder;
import org.projectodd.stilts.stomp.protocol.StompFrameEncoder;
import org.projectodd.stilts.stomp.protocol.websocket.WebSocketStompFrameDecoder;
import org.projectodd.stilts.stomp.protocol.websocket.WebSocketStompFrameEncoder;
import org.projectodd.stilts.stomp.server.protocol.ProtocolDetector;
import org.projectodd.stilts.stomp.server.websockets.protocol.HandshakeHandler;

public class ProtocolDetectorTest {

    private MockStompProvider mockProvider;
    private PipelineExposer pipelineExposer;
    private ProtocolDetector detector;
    private DecoderEmbedder<ChannelBuffer> decoder;

    @Before
    public void setUp() {
        this.mockProvider = new MockStompProvider();
        this.pipelineExposer = new PipelineExposer();
        this.detector = new ProtocolDetector( mockProvider, null );
        this.decoder = new DecoderEmbedder<ChannelBuffer>( this.pipelineExposer, this.detector );
    }

    @Test
    public void testSimpleStompDetection() throws Exception {
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes( "CONNECT\n".getBytes() );

        boolean result = this.decoder.offer( buffer );
        assertTrue( result );

        ChannelBuffer nextMessage = this.decoder.peek();
        String text = nextMessage.toString( Charset.forName( "UTF-8" ) );
        assertEquals( "CONNECT\n", text );

        ChannelPipeline pipeline = this.pipelineExposer.getPipeline();
        
        assertNull( pipeline.get( HttpRequestDecoder.class ) );
        assertNull( pipeline.get( HttpResponseEncoder.class ) );
        assertNull( pipeline.get( HandshakeHandler.class ) );
        assertNotNull( pipeline.get( StompFrameDecoder.class ) );
        assertNotNull( pipeline.get( StompFrameEncoder.class ) );
    }

    @Test
    public void testWebSocketDetection() throws Exception {
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes( "GET / HTTP/1.1\n".getBytes() );

        boolean result = this.decoder.offer( buffer );
        assertTrue( result );

        ChannelBuffer nextMessage = this.decoder.peek();
        String text = nextMessage.toString( Charset.forName( "UTF-8" ) );
        assertEquals( "GET / HTTP/1.1\n", text );

        ChannelPipeline pipeline = this.pipelineExposer.getPipeline();
        
        assertNotNull( pipeline.get( HttpRequestDecoder.class ) );
        assertNotNull( pipeline.get( HttpResponseEncoder.class ) );
        assertNotNull( pipeline.get( HandshakeHandler.class ) );
        assertNotNull( pipeline.get( WebSocketStompFrameDecoder.class ) );
        assertNotNull( pipeline.get( WebSocketStompFrameEncoder.class ) );
    }

}