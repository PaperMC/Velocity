/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;


public record ClientboundSoundEntityPacket(Sound sound, @Nullable Float fixedRange,
                                           int emitterEntityId) implements MinecraftPacket {

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<ClientboundSoundEntityPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ClientboundSoundEntityPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                                ProtocolVersion protocolVersion) {
      throw new UnsupportedOperationException("Decode is not implemented");
    }

    @Override
    public void encode(ClientboundSoundEntityPacket packet, ByteBuf buf,
                       ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeVarInt(buf, 0); // version-dependent, hardcoded sound ID

      ProtocolUtils.writeMinimalKey(buf, packet.sound.name());

      buf.writeBoolean(packet.fixedRange != null);
      if (packet.fixedRange != null) {
        buf.writeFloat(packet.fixedRange);
      }

      ProtocolUtils.writeSoundSource(buf, protocolVersion, packet.sound.source());

      ProtocolUtils.writeVarInt(buf, packet.emitterEntityId);

      buf.writeFloat(packet.sound.volume());

      buf.writeFloat(packet.sound.pitch());

      buf.writeLong(packet.sound.seed().orElse(ThreadLocalRandom.current().nextLong()));
    }
  }
}
