/*
 * Copyright (C) 2024 Velocity Contributors
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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;

import javax.annotation.Nullable;

public class ClientboundStopSoundPacket implements MinecraftPacket {

  private @Nullable Sound.Source source;
  private @Nullable Key soundName;

  public ClientboundStopSoundPacket() {}

  public ClientboundStopSoundPacket(SoundStop soundStop) {
    this(soundStop.source(), soundStop.sound());
  }

  public ClientboundStopSoundPacket(@Nullable Sound.Source source, @Nullable Key soundName) {
    this.source = source;
    this.soundName = soundName;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int flagsBitmask = buf.readByte();

    if ((flagsBitmask & 1) != 0) {
      source = Sound.Source.values()[ProtocolUtils.readVarInt(buf)];
    } else {
      source = null;
    }

    if ((flagsBitmask & 2) != 0) {
      soundName = ProtocolUtils.readKey(buf);
    } else {
      soundName = null;
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int flagsBitmask = 0;
    if (source != null && soundName == null) {
      flagsBitmask |= 1;
    } else if (soundName != null && source == null) {
      flagsBitmask |= 2;
    } else if (source != null /*&& sound != null*/) {
      flagsBitmask |= 3;
    }

    buf.writeByte(flagsBitmask);

    if (source != null) {
      ProtocolUtils.writeVarInt(buf, source.ordinal());
    }

    if (soundName != null) {
      ProtocolUtils.writeString(buf, soundName.asMinimalString()); // not using writeKey, as the client already defaults to the vanilla namespace
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Nullable
  public Sound.Source getSource() {
    return source;
  }

  @Nullable
  public Key getSoundName() {
    return soundName;
  }

}
