/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import static com.velocitypowered.proxy.network.Connections.FLOW_HANDLER;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.AutoReadHolderHandler;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;

/**
 * Backend channel initializer.
 */
@SuppressWarnings("WeakerAccess")
public class BackendChannelInitializer extends ChannelInitializer<Channel> {

  private final VelocityServer server;

  public BackendChannelInitializer(VelocityServer server) {
    this.server = server;
  }

  @Override
  protected void initChannel(Channel ch) {
    ch.pipeline()
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder(ProtocolUtils.Direction.CLIENTBOUND))
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER,
            new MinecraftDecoder(ProtocolUtils.Direction.CLIENTBOUND))
        .addLast(FLOW_HANDLER, new AutoReadHolderHandler())
        .addLast(MINECRAFT_ENCODER,
            new MinecraftEncoder(ProtocolUtils.Direction.SERVERBOUND));
  }
}
