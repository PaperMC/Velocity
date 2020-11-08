package com.velocitypowered.proxy.network.serialization.brigadier;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ArgumentPropertySerializer<T> {
  @Nullable T deserialize(ByteBuf buf);

  void serialize(T object, ByteBuf buf);
}
