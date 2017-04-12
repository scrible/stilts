package org.projectodd.stilts.stomp.protocol.websocket.ietf07;

import org.jboss.logging.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;
import org.projectodd.stilts.stomp.protocol.websocket.DefaultWebSocketFrame;
import org.projectodd.stilts.stomp.protocol.websocket.WebSocketFrame.FrameType;

public class Ietf07WebSocketFrameDecoder extends ReplayingDecoder<VoidEnum> {

    public static final int DEFAULT_MAX_FRAME_SIZE = 256*1024;

    private final int maxFrameSize;

    public Ietf07WebSocketFrameDecoder() {
        this( DEFAULT_MAX_FRAME_SIZE );
    }

    /**
     * Creates a new instance of {@code WebSocketFrameDecoder} with the
     * specified {@code maxFrameSize}. If the client
     * sends a frame size larger than {@code maxFrameSize}, the channel will be
     * closed.
     * 
     * @param maxFrameSize the maximum frame size to decode
     */
    public Ietf07WebSocketFrameDecoder(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, VoidEnum state) throws Exception {
        byte finOpcode = buffer.readByte();
        boolean fin = ((finOpcode & 0x1) != 0);
        int opcode = ( (finOpcode >> 4) & 0x0F );

        byte lengthMask = buffer.readByte();

        boolean masked = ((lengthMask & 0x80) != 0);

        long length = (lengthMask & 0x7F);

        if (length == 126) {
            length = buffer.readShort();
        } else if (length == 127) {
            length = buffer.readLong();
        }

        if (length > this.maxFrameSize) {
            throw new TooLongFrameException();
        }

        byte[] mask = null;

        if (masked) {
            mask = new byte[4];
            buffer.readBytes( mask );
        }

        byte[] payload = new byte[(int) length];

        buffer.readBytes( payload );

        if (masked) {
            for (int i = 0; i < payload.length; ++i) {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }

        ChannelBuffer data = ChannelBuffers.wrappedBuffer( payload );

        FrameType frameType = decodeFrameType( opcode );
        return new DefaultWebSocketFrame( frameType, data );

    }

    protected FrameType decodeFrameType(int opcode) {
        switch (opcode) {
        case 0x0:
            return FrameType.CONTINUATION;
        case 0x1:
            return FrameType.TEXT;
        case 0x2:
            return FrameType.BINARY;
        case 0x8:
            return FrameType.CLOSE;
        case 0x9:
            return FrameType.PING;
        case 0xA:
            return FrameType.PONG;
        }

        return null;
    }

    private static final Logger log = Logger.getLogger( Ietf07WebSocketFrameDecoder.class );

}
