/*
 * Copyright (C) 2018-2026 Velocity Contributors
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
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serializes the properties of {@code minecraft:time}: a minimum tick count before 26.4, optional
 * minimum and maximum bounds from 26.4 on.
 */
public class TimeArgumentSerializer implements ArgumentPropertySerializer<TimeArgumentSerializer.Bounds> {

  static final TimeArgumentSerializer TIME = new TimeArgumentSerializer();

  private static final int FLAG_MIN = 0x01;
  private static final int FLAG_MAX = 0x02;

  @Override
  public Bounds deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_26_4)) {
      byte flags = buf.readByte();
      Integer min = (flags & FLAG_MIN) != 0 ? buf.readInt() : null;
      Integer max = (flags & FLAG_MAX) != 0 ? buf.readInt() : null;
      return new Bounds(min, max);
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      return new Bounds(buf.readInt(), null);
    }
    return new Bounds(0, null);
  }

  @Override
  public void serialize(Bounds object, ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_26_4)) {
      buf.writeByte((object.min() != null ? FLAG_MIN : 0) | (object.max() != null ? FLAG_MAX : 0));
      if (object.min() != null) {
        buf.writeInt(object.min());
      }
      if (object.max() != null) {
        buf.writeInt(object.max());
      }
    } else if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      buf.writeInt(object.min() != null ? object.min() : 0);
    }
  }

  /**
   * The tick bounds of a time argument. Before 26.4 only {@code min} is sent.
   *
   * @param min the minimum tick count, if any
   * @param max the maximum tick count, if any (26.4+)
   */
  public record Bounds(@Nullable Integer min, @Nullable Integer max) {
  }
}
