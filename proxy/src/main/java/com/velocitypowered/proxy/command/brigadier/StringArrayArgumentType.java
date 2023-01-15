/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.command.brigadier;

import com.google.common.base.Splitter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * An argument type that parses the remaining contents of a {@link StringReader}, splitting the
 * input into words and placing the results in a string array.
 */
public final class StringArrayArgumentType implements ArgumentType<String[]> {

  public static final StringArrayArgumentType INSTANCE = new StringArrayArgumentType();
  public static final String[] EMPTY = new String[0];

  private static final Splitter WORD_SPLITTER =
      Splitter.on(CommandDispatcher.ARGUMENT_SEPARATOR_CHAR);
  private static final List<String> EXAMPLES = Arrays.asList("word", "some words");

  private StringArrayArgumentType() {
  }

  @Override
  public String[] parse(final StringReader reader) throws CommandSyntaxException {
    final String text = reader.getRemaining();
    reader.setCursor(reader.getTotalLength());
    if (text.isEmpty()) {
      return EMPTY;
    }
    return WORD_SPLITTER.splitToList(text).toArray(EMPTY);
  }

  @Override
  public String toString() {
    return "stringArray()";
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
