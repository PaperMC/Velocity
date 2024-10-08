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
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class ClientboundSoundEntityPacket implements MinecraftPacket {

  private static final Random SEEDS_RANDOM = new Random();

  private Sound sound;
  private @Nullable Float fixedRange;
  private int entityId;

  public ClientboundSoundEntityPacket() {}

  public ClientboundSoundEntityPacket(Sound sound, @Nullable Float fixedRange, int entityId) {
    this.sound = sound;
    this.fixedRange = fixedRange;
    this.entityId = entityId;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, 0); // version-dependent hardcoded sound id

    ProtocolUtils.writeString(buf, sound.name().asMinimalString()); // not using writeKey, as the client already defaults to the vanilla namespace

    buf.writeBoolean(fixedRange != null);
    if (fixedRange != null)
      buf.writeFloat(fixedRange);

    ProtocolUtils.writeVarInt(buf, sound.source().ordinal());

    ProtocolUtils.writeVarInt(buf, entityId);

    buf.writeFloat(sound.volume());

    buf.writeFloat(sound.pitch());

    buf.writeLong(sound.seed().orElse(SEEDS_RANDOM.nextLong()));
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

}
