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
    byte[] serialized = ProtocolUtils.readByteArray(buf);
    ByteBuf unrolled = Unpooled.wrappedBuffer(serialized);

    String name = ProtocolUtils.readString(unrolled);
    byte[] extraData = new byte[unrolled.readableBytes()];
    unrolled.readBytes(extraData);
    return new ModArgumentProperty(name, Unpooled.wrappedBuffer(extraData));
  }

  @Override
  public void serialize(ModArgumentProperty object, ByteBuf buf) {
    // This is special-cased by ArgumentPropertyRegistry
    throw new UnsupportedOperationException();
  }
}
