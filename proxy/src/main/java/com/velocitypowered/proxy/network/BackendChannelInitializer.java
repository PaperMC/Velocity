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

import static com.velocitypowered.proxy.network.HandlerNames.FLOW_HANDLER;
import static com.velocitypowered.proxy.network.HandlerNames.FRAME_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.READ_TIMEOUT;

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
        .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
        .addLast(READ_TIMEOUT,
            new ReadTimeoutHandler(server.configuration().getReadTimeout(),
                TimeUnit.MILLISECONDS))
        .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
        .addLast(MINECRAFT_DECODER,
            new MinecraftDecoder(PacketDirection.CLIENTBOUND))
        .addLast(FLOW_HANDLER, new AutoReadHolderHandler())
        .addLast(MINECRAFT_ENCODER,
            new MinecraftEncoder(PacketDirection.SERVERBOUND));
  }
}
