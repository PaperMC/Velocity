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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

import javax.annotation.Nullable;

public record ClientboundStopSoundPacket(@Nullable Sound.Source source,
                                         @Nullable Key soundName) implements MinecraftPacket {

  public ClientboundStopSoundPacket(SoundStop soundStop) {
    this(soundStop.source(), soundStop.sound());
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<ClientboundStopSoundPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ClientboundStopSoundPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                              ProtocolVersion protocolVersion) {
      int flagsBitmask = buf.readByte();

      Sound.Source source = null;
      if ((flagsBitmask & 1) != 0) {
        source = ProtocolUtils.readSoundSource(buf, protocolVersion);
      }

      Key soundName = null;
      if ((flagsBitmask & 2) != 0) {
        soundName = ProtocolUtils.readKey(buf);
      }

      return new ClientboundStopSoundPacket(source, soundName);
    }

    @Override
    public void encode(ClientboundStopSoundPacket packet, ByteBuf buf,
                       ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      int flagsBitmask = 0;
      if (packet.source != null && packet.soundName == null) {
        flagsBitmask |= 1;
      } else if (packet.soundName != null && packet.source == null) {
        flagsBitmask |= 2;
      } else if (packet.source != null /*&& sound != null*/) {
        flagsBitmask |= 3;
      }

      buf.writeByte(flagsBitmask);

      if (packet.source != null) {
        ProtocolUtils.writeSoundSource(buf, protocolVersion, packet.source);
      }

      if (packet.soundName != null) {
        ProtocolUtils.writeMinimalKey(buf, packet.soundName);
      }
    }
  }
}
