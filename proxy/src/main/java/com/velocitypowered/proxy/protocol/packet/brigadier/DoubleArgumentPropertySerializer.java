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

import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MAXIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MINIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.getFlags;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

class DoubleArgumentPropertySerializer implements ArgumentPropertySerializer<DoubleArgumentType> {

  static final DoubleArgumentPropertySerializer DOUBLE = new DoubleArgumentPropertySerializer();

  private DoubleArgumentPropertySerializer() {
  }

  @Override
  public DoubleArgumentType deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    byte flags = buf.readByte();
    double minimum = (flags & HAS_MINIMUM) != 0 ? buf.readDouble() : Double.MIN_VALUE;
    double maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readDouble() : Double.MAX_VALUE;
    return DoubleArgumentType.doubleArg(minimum, maximum);
  }

  @Override
  public void serialize(DoubleArgumentType object, ByteBuf buf, ProtocolVersion protocolVersion) {
    boolean hasMinimum = Double.compare(object.getMinimum(), Double.MIN_VALUE) != 0;
    boolean hasMaximum = Double.compare(object.getMaximum(), Double.MAX_VALUE) != 0;
    byte flag = getFlags(hasMinimum, hasMaximum);

    buf.writeByte(flag);
    if (hasMinimum) {
      buf.writeDouble(object.getMinimum());
    }
    if (hasMaximum) {
      buf.writeDouble(object.getMaximum());
    }
  }
}
