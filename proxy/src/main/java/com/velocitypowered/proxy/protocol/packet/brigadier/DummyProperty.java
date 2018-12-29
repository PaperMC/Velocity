package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;

class DummyProperty<T> implements ArgumentType<T> {

  private final String identifier;
  private final ArgumentPropertySerializer<T> serializer;
  @Nullable
  private final T result;

  DummyProperty(String identifier, ArgumentPropertySerializer<T> serializer, @Nullable T result) {
    this.identifier = identifier;
    this.serializer = serializer;
    this.result = result;
  }

  @Override
  public <S> T parse(StringReader reader) throws CommandSyntaxException {
    throw new UnsupportedOperationException();
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
}
