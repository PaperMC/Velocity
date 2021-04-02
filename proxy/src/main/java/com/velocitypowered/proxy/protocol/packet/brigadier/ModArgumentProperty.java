/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
