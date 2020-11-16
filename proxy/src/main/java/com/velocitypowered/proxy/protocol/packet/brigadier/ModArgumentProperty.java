package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModArgumentProperty implements ArgumentType<byte[]> {

  private final String identifier;
  private final byte[] data;

  public ModArgumentProperty(String identifier, byte[] data) {
    this.identifier = identifier;
    this.data = data;
  }

  public String getIdentifier() {
    return identifier;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public byte[] parse(StringReader reader) throws CommandSyntaxException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
      SuggestionsBuilder builder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<String> getExamples() {
    throw new UnsupportedOperationException();
  }
}
