package com.velocitypowered.proxy.protocol.packet.brigadier;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

class ByteArgumentPropertySerializer implements ArgumentPropertySerializer<Byte> {

  static final ByteArgumentPropertySerializer BYTE = new ByteArgumentPropertySerializer();

  private ByteArgumentPropertySerializer() {

  }

  @Nullable
  @Override
  public Byte deserialize(ByteBuf buf) {
    return buf.readByte();
  }

  @Override
  public void serialize(Byte object, ByteBuf buf) {
    buf.writeByte(object);
  }
}
