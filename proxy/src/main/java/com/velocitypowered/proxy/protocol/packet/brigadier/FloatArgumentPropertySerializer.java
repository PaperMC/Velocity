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

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

class FloatArgumentPropertySerializer implements ArgumentPropertySerializer<FloatArgumentType> {

  static final FloatArgumentPropertySerializer FLOAT = new FloatArgumentPropertySerializer();

  private FloatArgumentPropertySerializer() {

  }

  @Override
  public FloatArgumentType deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    byte flags = buf.readByte();
    float minimum = (flags & HAS_MINIMUM) != 0 ? buf.readFloat() : Float.MIN_VALUE;
    float maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readFloat() : Float.MAX_VALUE;
    return FloatArgumentType.floatArg(minimum, maximum);
  }

  @Override
  public void serialize(FloatArgumentType object, ByteBuf buf, ProtocolVersion protocolVersion) {
    boolean hasMinimum = Float.compare(object.getMinimum(), Float.MIN_VALUE) != 0;
    boolean hasMaximum = Float.compare(object.getMaximum(), Float.MAX_VALUE) != 0;
    byte flag = getFlags(hasMinimum, hasMaximum);

    buf.writeByte(flag);
    if (hasMinimum) {
      buf.writeFloat(object.getMinimum());
    }
    if (hasMaximum) {
      buf.writeFloat(object.getMaximum());
    }
  }
}
