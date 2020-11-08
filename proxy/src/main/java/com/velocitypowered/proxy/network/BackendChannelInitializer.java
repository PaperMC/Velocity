package com.velocitypowered.proxy.network;

import static com.velocitypowered.proxy.network.Connections.FLOW_HANDLER;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.pipeline.AutoReadHolderHandler;
import com.velocitypowered.proxy.network.pipeline.MinecraftDecoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftEncoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.network.pipeline.MinecraftVarintLengthEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class BackendChannelInitializer extends ChannelInitializer<Channel> {

  private final VelocityServer server;

  public BackendChannelInitializer(VelocityServer server) {
    this.server = server;
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ch.pipeline()
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER,
            new MinecraftDecoder(PacketDirection.CLIENTBOUND))
        .addLast(FLOW_HANDLER, new AutoReadHolderHandler())
        .addLast(MINECRAFT_ENCODER,
            new MinecraftEncoder(PacketDirection.SERVERBOUND));
  }
}
