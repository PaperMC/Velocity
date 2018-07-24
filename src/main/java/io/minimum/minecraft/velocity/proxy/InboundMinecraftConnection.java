package io.minimum.minecraft.velocity.proxy;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftDecoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftEncoder;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.proxy.handler.HandshakeSessionHandler;
import io.minimum.minecraft.velocity.proxy.handler.LoginSessionHandler;
import io.minimum.minecraft.velocity.proxy.handler.StatusSessionHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;

public class InboundMinecraftConnection {
    public static final AttributeKey<InboundMinecraftConnection> CONNECTION = AttributeKey.newInstance("velocity-connection");

    private final Channel channel;
    private boolean closed;
    private Handshake handshake;
    private StateRegistry state;
    private MinecraftSessionHandler sessionHandler;

    public InboundMinecraftConnection(Channel channel) {
        this.channel = channel;
        this.closed = false;
        this.state = StateRegistry.HANDSHAKE;
        this.sessionHandler = new HandshakeSessionHandler(this);
    }

    public void write(Object msg) {
        ensureOpen();
        channel.writeAndFlush(msg, channel.voidPromise());
    }

    public void closeWith(Object msg) {
        ensureOpen();
        closed = true;
        channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                future.channel().close();
            }
        });
    }

    public void close() {
        ensureOpen();
        channel.close();
        closed = true;
    }

    public MinecraftSessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void handleHandshake(Handshake handshake) {
        ensureOpen();
        Preconditions.checkNotNull(handshake, "handshake");
        Preconditions.checkState(this.handshake == null, "Already handled a handshake for this connection!");
        this.handshake = handshake;
        switch (handshake.getNextStatus()) {
            case 1:
                // Status protocol
                this.setStatus(StateRegistry.STATUS);
                this.sessionHandler = new StatusSessionHandler(this);
                break;
            case 2:
                this.setStatus(StateRegistry.LOGIN);
                this.sessionHandler = new LoginSessionHandler(this);
                break;
            default:
                throw new IllegalArgumentException("Invalid state " + handshake.getNextStatus());
        }
    }

    private void ensureOpen() {
        Preconditions.checkState(!closed, "Connection is closed.");
    }

    private void setStatus(StateRegistry state) {
        Preconditions.checkNotNull(state, "state");
        this.state = state;
        channel.pipeline().get(MinecraftEncoder.class).setState(state);
        channel.pipeline().get(MinecraftDecoder.class).setState(state);
    }

    public void teardown() {
        closed = true;
    }
}
