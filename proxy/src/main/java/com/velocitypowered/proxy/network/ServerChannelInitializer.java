package com.velocitypowered.proxy.network;

import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.LEGACY_PING_DECODER;
import static com.velocitypowered.proxy.network.Connections.LEGACY_PING_ENCODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.pipeline.LegacyPingDecoder;
import com.velocitypowered.proxy.network.pipeline.LegacyPingEncoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftDecoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftEncoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftVarintLengthEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class ServerChannelInitializer extends ChannelInitializer<Channel> {

  private final VelocityServer server;

  public ServerChannelInitializer(final VelocityServer server) {
    this.server = server;
  }

  @Override
  protected void initChannel(final Channel ch) {
    ch.pipeline()
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(this.server.getConfiguration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
        .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER, new MinecraftDecoder(PacketDirection.SERVERBOUND))
        .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(PacketDirection.CLIENTBOUND));

    final MinecraftConnection connection = new MinecraftConnection(ch, this.server);
    connection.setSessionHandler(new HandshakeSessionHandler(connection, this.server));
    ch.pipeline().addLast(Connections.HANDLER, connection);

    if (this.server.getConfiguration().isProxyProtocol()) {
      ch.pipeline().addFirst(new HAProxyMessageDecoder());
    }
  }
}
