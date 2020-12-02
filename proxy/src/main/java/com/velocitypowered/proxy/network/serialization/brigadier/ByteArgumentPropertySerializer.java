package com.velocitypowered.proxy.network.serialization.brigadier;

import io.netty.buffer.ByteBuf;

class ByteArgumentPropertySerializer implements ArgumentPropertySerializer<Byte> {

  static final ByteArgumentPropertySerializer BYTE = new ByteArgumentPropertySerializer();

  private ByteArgumentPropertySerializer() {

  }

  @Override
  public Byte deserialize(ByteBuf buf) {
    return buf.readByte();
  }

  @Override
  public void serialize(Byte object, ByteBuf buf) {
    buf.writeByte(object);
  }
}
