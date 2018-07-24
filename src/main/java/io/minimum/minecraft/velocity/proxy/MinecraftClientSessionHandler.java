package io.minimum.minecraft.velocity.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.minimum.minecraft.velocity.data.ServerPing;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.PacketWrapper;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftDecoder;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftEncoder;
import io.minimum.minecraft.velocity.protocol.packets.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.GsonComponentSerializer;

public class MinecraftClientSessionHandler extends ChannelInboundHandlerAdapter {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .create();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper) {
            try {
                handle(ctx, (PacketWrapper) msg);
            } finally {
                ((PacketWrapper) msg).getBuffer().release();
            }
        }

        if (msg instanceof LegacyPing) {
            System.out.println("Got LEGACY status request!");
            ServerPing ping = new ServerPing(
                    new ServerPing.Version(340, "1.12"),
                    new ServerPing.Players(0, 0),
                    TextComponent.of("this is a test"),
                    null
            );
            LegacyPingResponse response = LegacyPingResponse.from(ping);
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void handle(ChannelHandlerContext ctx, PacketWrapper msg) {
        MinecraftPacket packet = msg.getPacket();
        if (packet == null) {
            System.out.println("no packet!");
            return;
        }

        if (packet instanceof Handshake) {
            System.out.println("Handshake: " + packet);
            switch (((Handshake) packet).getNextStatus()) {
                case 1:
                    // status
                    ctx.pipeline().get(MinecraftDecoder.class).setState(StateRegistry.STATUS);
                    ctx.pipeline().get(MinecraftEncoder.class).setState(StateRegistry.STATUS);
                    break;
                case 2:
                    // login
                    throw new UnsupportedOperationException("Login not supported yet");
            }
        }

        if (packet instanceof StatusRequest) {
            System.out.println("Got status request!");
            ServerPing ping = new ServerPing(
                    new ServerPing.Version(340, "1.12.2"),
                    new ServerPing.Players(0, 0),
                    TextComponent.of("test"),
                    null
            );
            StatusResponse response = new StatusResponse();
            response.setStatus(GSON.toJson(ping));
            ctx.writeAndFlush(response, ctx.voidPromise());
        }

        if (packet instanceof Ping) {
            System.out.println("Ping: " + packet);
            ctx.writeAndFlush(packet).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
