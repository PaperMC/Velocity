package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

class ModArgumentPropertySerializer implements ArgumentPropertySerializer<ModArgumentProperty> {
  static final ModArgumentPropertySerializer MOD = new ModArgumentPropertySerializer();

  private ModArgumentPropertySerializer() {

  }

  @Override
  public @Nullable ModArgumentProperty deserialize(ByteBuf buf) {
    String name = ProtocolUtils.readString(buf);
    byte[] data = ProtocolUtils.readByteArray(buf);
    return new ModArgumentProperty(name, data);
  }

  @Override
  public void serialize(ModArgumentProperty object, ByteBuf buf) {
    // This is special-cased by ArgumentPropertyRegistry
    throw new UnsupportedOperationException();
  }
}
