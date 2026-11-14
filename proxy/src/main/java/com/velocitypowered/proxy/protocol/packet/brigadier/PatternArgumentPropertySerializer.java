/*
 * Copyright (C) 2026 Velocity Contributors
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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serializes the properties of {@code minecraft:pattern} (26.4+): a regex string and an
 * optional error message component.
 */
class PatternArgumentPropertySerializer
    implements ArgumentPropertySerializer<PatternArgumentPropertySerializer.Pattern> {

  static final PatternArgumentPropertySerializer PATTERN = new PatternArgumentPropertySerializer();

  private PatternArgumentPropertySerializer() {
  }

  @Override
  public Pattern deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    String pattern = ProtocolUtils.readString(buf);
    ComponentHolder errorMessage = buf.readBoolean()
        ? ComponentHolder.read(buf, protocolVersion) : null;
    return new Pattern(pattern, errorMessage);
  }

  @Override
  public void serialize(Pattern object, ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, object.pattern());
    buf.writeBoolean(object.errorMessage() != null);
    if (object.errorMessage() != null) {
      object.errorMessage().write(buf);
    }
  }

  record Pattern(String pattern, @Nullable ComponentHolder errorMessage) {
  }
}
