/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;

class ModArgumentPropertySerializer implements ArgumentPropertySerializer<ModArgumentProperty> {

  static final ModArgumentPropertySerializer MOD = new ModArgumentPropertySerializer();

  private ModArgumentPropertySerializer() {

  }

  @Override
  public @Nullable ModArgumentProperty deserialize(ByteBuf buf, ProtocolVersion version) {
    ArgumentIdentifier identifier;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      int idx = ProtocolUtils.readVarInt(buf);
      identifier = ArgumentIdentifier.id("crossstitch:identified_" + (idx < 0 ? "n" + (-idx) : idx),
          ArgumentIdentifier.mapSet(version, idx));
    } else {
      identifier = ArgumentIdentifier.id(ProtocolUtils.readString(buf));
    }

    byte[] extraData = ProtocolUtils.readByteArray(buf);
    return new ModArgumentProperty(identifier, Unpooled.wrappedBuffer(extraData));
  }

  @Override
  public void serialize(ModArgumentProperty object, ByteBuf buf, ProtocolVersion version) {
    // This is special-cased by ArgumentPropertyRegistry
    throw new UnsupportedOperationException();
  }
}
