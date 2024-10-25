package fun.iiii.mixedlogin.yggdrasil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

public class VirtualYggdrasilServer {
    private final int port;
    private final String ip;
    private final YggdrasilResultProcessor processor;

    private ChannelFuture f;
    public VirtualYggdrasilServer(int port, String ip, YggdrasilResultProcessor processor) {
        this.port = port;
        this.ip = ip;
        this.processor = processor;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(processor));

        f = bootstrap.bind(new InetSocketAddress(ip, port)).sync();
        System.out.println("yggdrasil server start up on : " + ip + ":" + port);
    }

    public ChannelFuture getChannelFuture() {
        return f;
    }
}

