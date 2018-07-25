package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.data.ServerPing;
import io.minimum.minecraft.velocity.protocol.PacketWrapper;
import io.minimum.minecraft.velocity.protocol.packets.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.kyori.text.TextComponent;

public class MinecraftClientSessionHandler extends ChannelInboundHandlerAdapter {
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
