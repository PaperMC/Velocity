package com.velocitypowered.proxy.protocol.packet.brigadier;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An argument property serializer that will serialize and deserialize nothing.
 */
class EmptyArgumentPropertySerializer implements ArgumentPropertySerializer<Void> {

  static final ArgumentPropertySerializer<Void> EMPTY =
      new EmptyArgumentPropertySerializer();

  private EmptyArgumentPropertySerializer() {
  }

  @Override
  public @Nullable Void deserialize(ByteBuf buf) {
    return null;
  }

  @Override
  public void serialize(Void object, ByteBuf buf) {

  }
}
