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

package com.velocitypowered.proxy.protocol.packet.brigadier.forge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.ModInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModIdArgumentProperty implements ArgumentType<String> {

  public ModIdArgumentProperty() {}

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    return reader.readUnquotedString();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
      SuggestionsBuilder builder) {
    S source = context.getSource();

    if (source instanceof Player) {
      ModInfo modInfo = ((Player) source).getModInfo().orElse(null);

      if (modInfo != null) {
        for (ModInfo.Mod mod : modInfo.getMods()) {
          builder.suggest(mod.getId());
        }

        return builder.buildFuture();
      }
    }

    return Suggestions.empty();
  }

  @Override
  public Collection<String> getExamples() {
    throw new UnsupportedOperationException();
  }
}
