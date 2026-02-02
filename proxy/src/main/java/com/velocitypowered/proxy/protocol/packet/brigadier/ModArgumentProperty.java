/*
 * Copyright (C) 2020-2022 Velocity Contributors
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

/**
 * Represents a mod-specific argument type with custom binary data attached.
 *
 * <p>This class allows external mods or extensions to define their own command argument
 * types, identified by a namespaced {@link ArgumentIdentifier} and accompanied by
 * serialized {@link ByteBuf} data.</p>
 *
 * <p>Note: This type is not parseable or suggestible through Brigadier and exists primarily
 * to preserve compatibility with extended command metadata during serialization and
 * deserialization.</p>
 */
public class ModArgumentProperty implements ArgumentType<ByteBuf> {

  private final ArgumentIdentifier identifier;
  private final ByteBuf data;

  public ModArgumentProperty(ArgumentIdentifier identifier, ByteBuf data) {
    this.identifier = identifier;
    this.data = Unpooled.unreleasableBuffer(data.asReadOnly());
  }

  public ArgumentIdentifier getIdentifier() {
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
