/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

class IntegerArgumentPropertySerializer implements ArgumentPropertySerializer<IntegerArgumentType> {

  static final IntegerArgumentPropertySerializer INTEGER = new IntegerArgumentPropertySerializer();

  static final byte HAS_MINIMUM = 0x01;
  static final byte HAS_MAXIMUM = 0x02;

  private IntegerArgumentPropertySerializer() {

  }

  @Override
  public IntegerArgumentType deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    byte flags = buf.readByte();
    int minimum = (flags & HAS_MINIMUM) != 0 ? buf.readInt() : Integer.MIN_VALUE;
    int maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readInt() : Integer.MAX_VALUE;
    return IntegerArgumentType.integer(minimum, maximum);
  }

  @Override
  public void serialize(IntegerArgumentType object, ByteBuf buf, ProtocolVersion protocolVersion) {
    boolean hasMinimum = object.getMinimum() != Integer.MIN_VALUE;
    boolean hasMaximum = object.getMaximum() != Integer.MAX_VALUE;
    byte flag = getFlags(hasMinimum, hasMaximum);

    buf.writeByte(flag);
    if (hasMinimum) {
      buf.writeInt(object.getMinimum());
    }
    if (hasMaximum) {
      buf.writeInt(object.getMaximum());
    }
  }

  static byte getFlags(boolean hasMinimum, boolean hasMaximum) {
    byte flags = 0;
    if (hasMinimum) {
      flags |= HAS_MINIMUM;
    }
    if (hasMaximum) {
      flags |= HAS_MAXIMUM;
    }
    return flags;
  }
}
