package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

class DummyProperty<T> implements ArgumentType<T> {

  private final String identifier;
  private final ArgumentPropertySerializer<T> serializer;
  private final @Nullable T result;

  DummyProperty(String identifier, ArgumentPropertySerializer<T> serializer, @Nullable T result) {
    this.identifier = identifier;
    this.serializer = serializer;
    this.result = result;
  }

  public String getIdentifier() {
    return identifier;
  }

  public ArgumentPropertySerializer<T> getSerializer() {
    return serializer;
  }

  public @Nullable T getResult() {
    return result;
  }

  @Override
  public T parse(StringReader reader) {
    throw new UnsupportedOperationException();
  }
}
