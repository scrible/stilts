package org.jboss.netty.channel.socket.nio;

public class NioSocketChannelWriteBufferInspector {
    private AbstractNioChannel socketChannel;

    public NioSocketChannelWriteBufferInspector(AbstractNioChannel nioChannel) {
        this.socketChannel = nioChannel;
    }

    public int getWriteBufferPendingSize() {
        return this.socketChannel.writeBufferSize.get();
    }
}
