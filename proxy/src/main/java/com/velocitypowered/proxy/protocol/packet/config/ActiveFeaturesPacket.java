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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;

public record ActiveFeaturesPacket(Key[] activeFeatures) implements MinecraftPacket {

  public ActiveFeaturesPacket() {
    this(new Key[0]);
  }

  public Key[] getActiveFeatures() {
    return activeFeatures;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<ActiveFeaturesPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ActiveFeaturesPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                       ProtocolVersion protocolVersion) {
      Key[] activeFeatures = ProtocolUtils.readKeyArray(buf);
      return new ActiveFeaturesPacket(activeFeatures);
    }

    @Override
    public void encode(ActiveFeaturesPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion protocolVersion) {
      ProtocolUtils.writeKeyArray(buf, packet.activeFeatures);
    }
  }
}
