package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModArgumentProperty implements ArgumentType<ByteBuf> {

  private final String identifier;
  private final ByteBuf data;

  public ModArgumentProperty(String identifier, ByteBuf data) {
    this.identifier = identifier;
    this.data = Unpooled.unreleasableBuffer(data.asReadOnly());
  }

  public String getIdentifier() {
    return identifier;
  }

  public ByteBuf getData() {
    return data.slice();
  }

  @Override
  public ByteBuf parse(StringReader reader) throws CommandSyntaxException {
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
