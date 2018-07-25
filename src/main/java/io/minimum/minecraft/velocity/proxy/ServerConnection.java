package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.PacketWrapper;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftDecoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftEncoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftPipelineUtils;
import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import net.kyori.text.TextComponent;

public class ServerConnection {
    private Channel channel;
    private final ServerInfo info;
    private final ConnectedPlayer proxyPlayer;
    private StateRegistry registry;
    private final VelocityServer server;

    public ServerConnection(ServerInfo target, ConnectedPlayer proxyPlayer, VelocityServer server) {
        this.info = target;
        this.proxyPlayer = proxyPlayer;
        this.server = server;
        this.registry = StateRegistry.HANDSHAKE;
    }

    public void connect() {
        server.initializeGenericBootstrap()
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        MinecraftPipelineUtils.strapPipelineForProxy(ch);
                        ch.pipeline().addLast("state-based-interceptor", new StateBasedInterceptor());
                    }
                })
                .connect(info.getAddress())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        channel = future.channel();
                    }
                });
    }

    public void disconnect() {
        channel.close();
        channel = null;
    }

    public void forward(ByteBuf buf) {
        if (registry != StateRegistry.PLAY) {
            throw new IllegalStateException("Not accepting player information until PLAY state");
        }
        channel.writeAndFlush(buf.retain());
    }

    private class StateBasedInterceptor extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // Initiate a handshake.
            Handshake handshake = new Handshake();
            handshake.setNextStatus(2); // login
            handshake.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion()); // TODO: Expose client version
            handshake.setServerAddress(info.getAddress().getHostString());
            handshake.setPort(info.getAddress().getPort());
            ctx.writeAndFlush(handshake, ctx.voidPromise());

            setRegistry(StateRegistry.LOGIN);

            // Login
            ServerLogin login = new ServerLogin();
            login.setUsername(proxyPlayer.getUsername());
            ctx.writeAndFlush(login, ctx.voidPromise());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (proxyPlayer.getConnection().isClosed()) {
                // The upstream connection is closed, but we didn't forward that on for some reason. Close the connection
                // here.
                ctx.close();
                return;
            }

            if (msg instanceof PacketWrapper) {
                PacketWrapper pw = (PacketWrapper) msg;
                try {
                    switch (registry) {
                        case LOGIN:
                            onLogin(ctx, pw);
                            break;
                        case PLAY:
                            onPlay(ctx, pw);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported state " + registry);
                    }
                } finally {
                    ((PacketWrapper) msg).getBuffer().release();
                }
            }
        }

        private void onPlay(ChannelHandlerContext ctx, PacketWrapper pw) {
            proxyPlayer.getConnection().write(pw.getBuffer().retain());
        }

        private void onLogin(ChannelHandlerContext ctx, PacketWrapper wrapper) {
            //System.out.println("FROM PROXIED SERVER -> " + wrapper.getPacket() + " / " + ByteBufUtil.hexDump(wrapper.getBuffer()));
            MinecraftPacket packet = wrapper.getPacket();
            if (packet instanceof Disconnect) {
                Disconnect disconnect = (Disconnect) packet;
                ctx.close();
                proxyPlayer.handleConnectionException(disconnect);
            }

            if (packet instanceof ServerLoginSuccess) {
                // the player has been logged on.
                System.out.println("Player connected to remote server");
                setRegistry(StateRegistry.PLAY);
                proxyPlayer.setConnectedServer(ServerConnection.this);
            }
        }
    }

    private void setRegistry(StateRegistry registry) {
        this.registry = registry;
        this.channel.pipeline().get(MinecraftEncoder.class).setState(registry);
        this.channel.pipeline().get(MinecraftDecoder.class).setState(registry);
    }
}
