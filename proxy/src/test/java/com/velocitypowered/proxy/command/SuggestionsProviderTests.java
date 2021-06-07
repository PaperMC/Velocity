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

package com.velocitypowered.proxy.command;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Command} implementation-independent suggestion methods of
 * {@link SuggestionsProvider}.
 */
public class SuggestionsProviderTests extends CommandTestSuite {

  @Test
  void testSuggestsAliasForEmptyInput() {
    final var meta = manager.metaBuilder("foo")
            .aliases("bar", "baz")
            .build();
    manager.register(meta, NoSuggestionsCommand.INSTANCE);

    assertSuggestions("", "bar", "baz", "foo");
  }

  @Test
  void testDoesNotSuggestForLeadingWhitespace() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, NoSuggestionsCommand.INSTANCE);

    assertSuggestions(" ");
  }

  @Test
  void testSuggestsAliasesForPartialAlias() {
    final var meta = manager.metaBuilder("foo")
            .aliases("bar", "baz")
            .build();
    manager.register(meta, NoSuggestionsCommand.INSTANCE);

    assertSuggestions("ba", "bar", "baz");
    assertSuggestions("fo", "foo");
    assertSuggestions("bar");
  }

  @Test
  void testSuggestsHintLiteral() {
    final var hint = LiteralArgumentBuilder
            .<CommandSource>literal("hint")
            .build();
    final var meta = manager.metaBuilder("hello")
            .hint(hint)
            .build();
    manager.register(meta, NoSuggestionsCommand.INSTANCE);

    assertSuggestions("hello ", "hint");
    assertSuggestions("hello hin", "hint");
    assertSuggestions("hello hint");
  }

  @Test
  void testSuggestsHintCustomSuggestions() {
    final var hint = RequiredArgumentBuilder
            .<CommandSource, String>argument("hint", word())
            .suggests((context, builder) -> builder
                    .suggest("one")
                    .suggest("two")
                    .suggest("three")
                    .buildFuture())
            .build();
    final var meta = manager.metaBuilder("hello")
            .hint(hint)
            .build();
    manager.register(meta, NoSuggestionsCommand.INSTANCE);

    assertSuggestions("hello ", "one", "three", "two");
    assertSuggestions("hello two", "one", "three");
  }

  @Test
  void testSuggestsMergesArgumentsSuggestionsWithHintSuggestions() {
    final var hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final var meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return ImmutableList.of("baz", "qux");
      }
    });

    // TODO Fix merging
    assertSuggestions("hello ", "bar", "baz", "qux");
    assertSuggestions("hello bar", "baz", "qux");
  }

  static final class NoSuggestionsCommand implements RawCommand {

    static final NoSuggestionsCommand INSTANCE = new NoSuggestionsCommand();

    private NoSuggestionsCommand() {}

    @Override
    public void execute(final Invocation invocation) {
      fail();
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
      return ImmutableList.of();
    }
  }
}
