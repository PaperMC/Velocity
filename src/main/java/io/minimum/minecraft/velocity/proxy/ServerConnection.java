package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.PacketWrapper;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftDecoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftEncoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftPipelineUtils;
import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.netty.channel.*;
import net.kyori.text.serializer.ComponentSerializers;

import java.io.IOException;

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

    private class StateBasedInterceptor extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // Initiate a handshake.
            Handshake handshake = new Handshake();
            handshake.setNextStatus(2); // login
            handshake.setProtocolVersion(ProtocolConstants.MINECRAFT_1_12); // TODO: Expose client version
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
            if (msg instanceof PacketWrapper) {
                PacketWrapper pw = (PacketWrapper) msg;
                try {
                    switch (registry) {
                        case LOGIN:
                            onLogin(ctx, pw);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported state " + registry);
                    }
                } finally {
                    ((PacketWrapper) msg).getBuffer().release();
                }
            }
        }

        private void onLogin(ChannelHandlerContext ctx, PacketWrapper wrapper) {
            MinecraftPacket packet = wrapper.getPacket();
            if (packet instanceof Disconnect) {
                Disconnect disconnect = (Disconnect) packet;
                ctx.close();
                proxyPlayer.handleConnectionException(new IOException("Disconnected from target: " + jsonToPlain(disconnect.getReason())));
            }

            if (packet instanceof ServerLoginSuccess) {
                System.out.println("got it");
            }
        }
    }

    private void setRegistry(StateRegistry registry) {
        this.registry = registry;
        this.channel.pipeline().get(MinecraftEncoder.class).setState(registry);
        this.channel.pipeline().get(MinecraftDecoder.class).setState(registry);
    }

    private static String jsonToPlain(String j) {
        return ComponentSerializers.LEGACY.serialize(
                ComponentSerializers.JSON.deserialize(j)
        ).replaceAll("\\u00A7[a-z0-9]", "");
    }
}
