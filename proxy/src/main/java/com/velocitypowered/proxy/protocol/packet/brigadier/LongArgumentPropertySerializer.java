package com.velocitypowered.proxy.protocol.packet.brigadier;

import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MAXIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.HAS_MINIMUM;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.getFlags;

import com.mojang.brigadier.arguments.LongArgumentType;
import io.netty.buffer.ByteBuf;

class LongArgumentPropertySerializer implements ArgumentPropertySerializer<LongArgumentType> {

  static final LongArgumentPropertySerializer LONG = new LongArgumentPropertySerializer();

  private LongArgumentPropertySerializer() {

  }

  @Override
  public LongArgumentType deserialize(ByteBuf buf) {
    byte flags = buf.readByte();
    long minimum = (flags & HAS_MINIMUM) != 0 ? buf.readLong() : Long.MIN_VALUE;
    long maximum = (flags & HAS_MAXIMUM) != 0 ? buf.readLong() : Long.MAX_VALUE;
    return LongArgumentType.longArg(minimum, maximum);
  }

  @Override
  public void serialize(LongArgumentType object, ByteBuf buf) {
    boolean hasMinimum = object.getMinimum() != Long.MIN_VALUE;
    boolean hasMaximum = object.getMaximum() != Long.MAX_VALUE;
    byte flag = getFlags(hasMinimum, hasMaximum);

    buf.writeByte(flag);
    if (hasMinimum) {
      buf.writeLong(object.getMinimum());
    }
    if (hasMaximum) {
      buf.writeLong(object.getMaximum());
    }
  }
}
