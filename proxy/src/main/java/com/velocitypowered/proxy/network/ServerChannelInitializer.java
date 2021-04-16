/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.network;

import static com.velocitypowered.proxy.network.HandlerNames.FRAME_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.LEGACY_PING_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.LEGACY_PING_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.READ_TIMEOUT;

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
        .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(this.server.getConfiguration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER, new MinecraftDecoder(PacketDirection.SERVERBOUND))
        .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(PacketDirection.CLIENTBOUND));

    final MinecraftConnection connection = new MinecraftConnection(ch, this.server);
    connection.setSessionHandler(new HandshakeSessionHandler(connection, this.server));
    ch.pipeline().addLast(HandlerNames.HANDLER, connection);

    if (this.server.getConfiguration().isProxyProtocol()) {
      ch.pipeline().addFirst(new HAProxyMessageDecoder());
    }
  }
}
