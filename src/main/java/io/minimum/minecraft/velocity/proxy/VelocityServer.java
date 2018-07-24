package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.netty.*;
import io.minimum.minecraft.velocity.proxy.server.ServerConnection;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class VelocityServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup childGroup;

    public VelocityServer() {

    }

    public void initialize() {
        bossGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();
        new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(bossGroup, childGroup)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.attr(InboundMinecraftConnection.CONNECTION).set(new InboundMinecraftConnection(ch));
                        MinecraftPipelineUtils.strapPipeline(ch);
                        ch.pipeline().addLast("handler", new MinecraftClientSessionHandler());
                    }
                })
                .bind(26671)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("Listening on " + future.channel().localAddress());
                        } else {
                            System.out.println("Can't bind to " + future.channel().localAddress());
                            future.cause().printStackTrace();
                        }
                    }
                });
    }

    Bootstrap initializeGenericBootstrap() {
        return new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(childGroup);
    }
}
