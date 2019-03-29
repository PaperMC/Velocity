package com.velocitypowered.proxy.protocol.packet.brigadier;

import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MAXIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MINIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.getFlags;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import io.netty.buffer.ByteBuf;

class DoubleArgumentPropertySerializer implements ArgumentPropertySerializer<DoubleArgumentType> {

  static final DoubleArgumentPropertySerializer DOUBLE = new DoubleArgumentPropertySerializer();

  private DoubleArgumentPropertySerializer() {
  }

  @Override
  public DoubleArgumentType deserialize(ByteBuf buf) {
    byte flags = buf.readByte();
    double minimum = (flags & HAS_MINIMUM) != 0 ? buf.readDouble() : Double.MIN_VALUE;
    double maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readDouble() : Double.MAX_VALUE;
    return DoubleArgumentType.doubleArg(minimum, maximum);
  }

  @Override
  public void serialize(DoubleArgumentType object, ByteBuf buf) {
    boolean hasMinimum = object.getMinimum() != Double.MIN_VALUE;
    boolean hasMaximum = object.getMaximum() != Double.MAX_VALUE;
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
