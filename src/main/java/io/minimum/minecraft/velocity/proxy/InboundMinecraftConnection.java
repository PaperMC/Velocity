package io.minimum.minecraft.velocity.proxy;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftDecoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftEncoder;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.minimum.minecraft.velocity.proxy.handler.HandshakeSessionHandler;
import io.minimum.minecraft.velocity.proxy.handler.LoginSessionHandler;
import io.minimum.minecraft.velocity.proxy.handler.StatusSessionHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.Optional;

public class InboundMinecraftConnection {
    public static final AttributeKey<InboundMinecraftConnection> CONNECTION = AttributeKey.newInstance("velocity-connection");

    private final Channel channel;
    private boolean closed;
    private Handshake handshake;
    private StateRegistry state;
    private MinecraftSessionHandler sessionHandler;
    private ConnectedPlayer connectedPlayer;

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

    public boolean isClosed() {
        return closed;
    }

    public Optional<ConnectedPlayer> getConnectedPlayer() {
        return Optional.ofNullable(connectedPlayer);
    }

    public int getProtocolVersion() {
        return handshake == null ? ProtocolConstants.MINECRAFT_1_12 : handshake.getProtocolVersion();
    }

    public void initiatePlay(ServerLoginSuccess success) {
        setStatus(StateRegistry.PLAY);
        ConnectedPlayer player = new ConnectedPlayer(success.getUsername(), success.getUuid(), this);
        ServerInfo info = new ServerInfo("test", new InetSocketAddress("127.0.0.1", 25565));
        ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());
        connection.connect();
    }
}
