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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Random;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.Nullable;

/**
 * A clientbound packet that instructs the client to play a sound tied to an entity.
 *
 * <p>This is sent by the server when a sound should be played at the location of a
 * specific entity, with optional fixed range and seed.</p>
 */
public class ClientboundSoundEntityPacket implements MinecraftPacket {

  private static final Random SEEDS_RANDOM = new Random();

  private Sound sound;
  private @Nullable Float fixedRange;
  private int emitterEntityId;

  public ClientboundSoundEntityPacket() {
  }

  /**
   * Constructs a new sound entity packet.
   *
   * @param sound the sound to play
   * @param fixedRange the fixed attenuation range, or {@code null} to use the default
   * @param emitterEntityId the entity ID of the sound emitter
   */
  public ClientboundSoundEntityPacket(Sound sound, @Nullable Float fixedRange, int emitterEntityId) {
    this.sound = sound;
    this.fixedRange = fixedRange;
    this.emitterEntityId = emitterEntityId;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, 0); // version-dependent, hardcoded sound ID

    ProtocolUtils.writeMinimalKey(buf, sound.name());

    buf.writeBoolean(fixedRange != null);
    if (fixedRange != null) {
      buf.writeFloat(fixedRange);
    }

    ProtocolUtils.writeSoundSource(buf, protocolVersion, sound.source());

    ProtocolUtils.writeVarInt(buf, emitterEntityId);

    buf.writeFloat(sound.volume());

    buf.writeFloat(sound.pitch());

    buf.writeLong(sound.seed().orElse(SEEDS_RANDOM.nextLong()));
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public Sound getSound() {
    return sound;
  }

  public void setSound(Sound sound) {
    this.sound = sound;
  }

  public @Nullable Float getFixedRange() {
    return fixedRange;
  }

  public void setFixedRange(@Nullable Float fixedRange) {
    this.fixedRange = fixedRange;
  }

  public int getEmitterEntityId() {
    return emitterEntityId;
  }

  public void setEmitterEntityId(int emitterEntityId) {
    this.emitterEntityId = emitterEntityId;
  }

}
