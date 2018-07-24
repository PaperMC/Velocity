package io.minimum.minecraft.velocity;

import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.netty.*;
import io.minimum.minecraft.velocity.proxy.MinecraftClientSessionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Velocity {
    public static void main(String... args) throws InterruptedException {
        new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(new NioEventLoopGroup())
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast("legacy-ping-decode", new LegacyPingDecoder());
                        ch.pipeline().addLast("frame-decoder", new MinecraftVarintFrameDecoder());
                        ch.pipeline().addLast("legacy-ping-encode", new LegacyPingEncoder());
                        ch.pipeline().addLast("frame-encoder", new MinecraftVarintLengthEncoder());
                        ch.pipeline().addLast("minecraft-decoder", new MinecraftDecoder(ProtocolConstants.Direction.TO_SERVER));
                        ch.pipeline().addLast("minecraft-encoder", new MinecraftEncoder(ProtocolConstants.Direction.TO_CLIENT));
                        ch.pipeline().addLast("handler", new MinecraftClientSessionHandler());
                    }
                })
                .bind(26671)
                .await();
    }
}
