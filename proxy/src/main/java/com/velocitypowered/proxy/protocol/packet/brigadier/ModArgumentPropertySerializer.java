package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;

class ModArgumentPropertySerializer implements ArgumentPropertySerializer<ModArgumentProperty> {
  static final ModArgumentPropertySerializer MOD = new ModArgumentPropertySerializer();

  private ModArgumentPropertySerializer() {

  }

  @Override
  public @Nullable ModArgumentProperty deserialize(ByteBuf buf) {
    String identifier = ProtocolUtils.readString(buf);
    byte[] extraData = ProtocolUtils.readByteArray(buf);
    return new ModArgumentProperty(identifier, Unpooled.wrappedBuffer(extraData));
  }

  @Override
  public void serialize(ModArgumentProperty object, ByteBuf buf) {
    // This is special-cased by ArgumentPropertyRegistry
    throw new UnsupportedOperationException();
  }
}
