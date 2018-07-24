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
        InboundMinecraftConnection connection = ctx.channel().attr(InboundMinecraftConnection.CONNECTION).get();
        if (msg instanceof PacketWrapper) {
            PacketWrapper pw = (PacketWrapper) msg;
            try {
                if (pw.getPacket() == null) {
                    connection.getSessionHandler().handleUnknown(pw.getBuffer());
                } else {
                    connection.getSessionHandler().handle(pw.getPacket());
                }
            } finally {
                ((PacketWrapper) msg).getBuffer().release();
            }
        }

        if (msg instanceof LegacyPing) {
            // TODO: port this
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InboundMinecraftConnection connection = ctx.channel().attr(InboundMinecraftConnection.CONNECTION).get();
        connection.teardown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
