package com.velocitypowered.proxy.protocol.packet.brigadier;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An argument property serializer that will serialize and deserialize nothing.
 */
class DummyVoidArgumentPropertySerializer implements ArgumentPropertySerializer<Void> {

  static final ArgumentPropertySerializer<Void> DUMMY =
      new DummyVoidArgumentPropertySerializer();

  private DummyVoidArgumentPropertySerializer() {
  }

  @Override
  public @Nullable Void deserialize(ByteBuf buf) {
    return null;
  }

  @Override
  public void serialize(Void object, ByteBuf buf) {

  }
}
