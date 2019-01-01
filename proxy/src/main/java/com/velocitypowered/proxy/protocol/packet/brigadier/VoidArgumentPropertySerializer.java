package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

class VoidArgumentPropertySerializer<T extends ArgumentType<?>>
    implements ArgumentPropertySerializer<T> {

  private final Supplier<T> argumentSupplier;

  private VoidArgumentPropertySerializer(Supplier<T> argumentSupplier) {
    this.argumentSupplier = argumentSupplier;
  }

  public static <T extends ArgumentType<?>> ArgumentPropertySerializer<T> create(
      Supplier<T> supplier) {
    return new VoidArgumentPropertySerializer<T>(supplier);
  }

  @Nullable
  @Override
  public T deserialize(ByteBuf buf) {
    return argumentSupplier.get();
  }

  @Override
  public void serialize(T object, ByteBuf buf) {

  }
}
