package com.velocitypowered.proxy.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.PacketWrapper;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.compression.JavaVelocityCompressor;
import com.velocitypowered.proxy.protocol.netty.*;
import com.velocitypowered.proxy.protocol.packets.SetCompression;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import static com.velocitypowered.proxy.protocol.netty.MinecraftPipelineUtils.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.protocol.netty.MinecraftPipelineUtils.MINECRAFT_ENCODER;

/**
 * A utility class to make working with the pipeline a little less painful and transparently handles certain Minecraft
 * protocol mechanics.
 */
public class MinecraftConnection extends ChannelInboundHandlerAdapter {
    private final Channel channel;
    private boolean closed;
    private StateRegistry state;
    private MinecraftSessionHandler sessionHandler;
    private int protocolVersion;

    public MinecraftConnection(Channel channel) {
        this.channel = channel;
        this.closed = false;
        this.state = StateRegistry.HANDSHAKE;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (sessionHandler != null) {
            sessionHandler.connected();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (sessionHandler != null) {
            sessionHandler.disconnected();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper) {
            PacketWrapper pw = (PacketWrapper) msg;
            try {
                if (sessionHandler != null) {
                    if (pw.getPacket() == null) {
                        sessionHandler.handleUnknown(pw.getBuffer());
                    } else {
                        sessionHandler.handle(pw.getPacket());
                    }
                }
            } finally {
                ReferenceCountUtil.release(pw.getBuffer());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx.channel().isActive()) {
            cause.printStackTrace();

            if (sessionHandler != null) {
                sessionHandler.exception(cause);
            }

            closed = true;
            ctx.close();
        }
    }

    public void write(Object msg) {
        ensureOpen();
        channel.writeAndFlush(msg, channel.voidPromise());
    }

    public void closeWith(Object msg) {
        ensureOpen();
        teardown();
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
    }

    public void close() {
        ensureOpen();
        teardown();
        channel.close();
    }

    public void teardown() {
        closed = true;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isClosed() {
        return closed;
    }

    public StateRegistry getState() {
        return state;
    }

    public void setState(StateRegistry state) {
        this.state = state;
        this.channel.pipeline().get(MinecraftEncoder.class).setState(state);
        this.channel.pipeline().get(MinecraftDecoder.class).setState(state);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.channel.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocolVersion);
        this.channel.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocolVersion);
    }

    public MinecraftSessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void setSessionHandler(MinecraftSessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
        sessionHandler.activated();
    }

    private void ensureOpen() {
        Preconditions.checkState(!closed, "Connection is closed.");
    }

    public void setCompressionThreshold(int threshold) {
        channel.writeAndFlush(new SetCompression(threshold), channel.voidPromise());

        if (threshold == -1) {
            channel.pipeline().remove("compress-decoder");
            channel.pipeline().remove("compress-encoder");
            return;
        }

        JavaVelocityCompressor compressor = new JavaVelocityCompressor();
        MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(threshold, compressor);
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(threshold, compressor);

        channel.pipeline().addBefore(MINECRAFT_DECODER, "compress-decoder", decoder);
        channel.pipeline().addBefore(MINECRAFT_ENCODER, "compress-encoder", encoder);
    }
}
