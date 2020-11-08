package com.velocitypowered.proxy.network.serialization.brigadier;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.netty.buffer.ByteBuf;

class IntegerArgumentPropertySerializer implements ArgumentPropertySerializer<IntegerArgumentType> {

  static final IntegerArgumentPropertySerializer INTEGER = new IntegerArgumentPropertySerializer();

  static final byte HAS_MINIMUM = 0x01;
  static final byte HAS_MAXIMUM = 0x02;

  private IntegerArgumentPropertySerializer() {

  }

  @Override
  public IntegerArgumentType deserialize(ByteBuf buf) {
    byte flags = buf.readByte();
    int minimum = (flags & HAS_MINIMUM) != 0 ? buf.readInt() : Integer.MIN_VALUE;
    int maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readInt() : Integer.MAX_VALUE;
    return IntegerArgumentType.integer(minimum, maximum);
  }

  @Override
  public void serialize(IntegerArgumentType object, ByteBuf buf) {
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
