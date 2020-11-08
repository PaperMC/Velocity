package com.velocitypowered.proxy.network.serialization.brigadier;

import static com.velocitypowered.proxy.network.serialization.brigadier.IntegerArgumentPropertySerializer.HAS_MAXIMUM;
import static com.velocitypowered.proxy.network.serialization.brigadier.IntegerArgumentPropertySerializer.HAS_MINIMUM;
import static com.velocitypowered.proxy.network.serialization.brigadier.IntegerArgumentPropertySerializer.getFlags;

import com.mojang.brigadier.arguments.FloatArgumentType;
import io.netty.buffer.ByteBuf;

class FloatArgumentPropertySerializer implements ArgumentPropertySerializer<FloatArgumentType> {

  static final FloatArgumentPropertySerializer FLOAT = new FloatArgumentPropertySerializer();

  private FloatArgumentPropertySerializer() {

  }

  @Override
  public FloatArgumentType deserialize(ByteBuf buf) {
    byte flags = buf.readByte();
    float minimum = (flags & HAS_MINIMUM) != 0 ? buf.readFloat() : Float.MIN_VALUE;
    float maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readFloat() : Float.MAX_VALUE;
    return FloatArgumentType.floatArg(minimum, maximum);
  }

  @Override
  public void serialize(FloatArgumentType object, ByteBuf buf) {
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
