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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class BrigadierCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testAliasLevelNodeExecutionDoesNotForward() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> literal = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .executes(context -> {
              assertEquals(source, context.getSource());
              assertEquals("hello", context.getInput());
              assertEquals(1, context.getNodes().size());
              callCount.incrementAndGet();
              return 1;
            })
            .build();
    manager.register(new BrigadierCommand(literal));

    assertNotForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testForwardedWithImpermissibleAliasLevelNode() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .executes(context -> fail())
            .requires(actualSource -> {
              assertEquals(source, actualSource);
              callCount.incrementAndGet();
              return false;
            })
            .build();
    manager.register(new BrigadierCommand(node));

    assertForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testForwardedWithContextImpermissibleAliasLevelNode() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .executes(context -> fail())
            .requiresWithContext((context, reader) -> {
              assertEquals(source, context.getSource());
              assertEquals("hello", reader.getRead());
              assertEquals(1, context.getNodes().size());
              callCount.incrementAndGet();
              return false;
            })
            .build();
    manager.register(new BrigadierCommand(node));

    assertForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNonAliasLevelNodeExecutionDoesNotForward() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("buy")
            .executes(context -> fail())
            .then(RequiredArgumentBuilder
                  .<CommandSource, Integer>argument("quantity", IntegerArgumentType.integer())
                  .executes(context -> {
                    assertEquals("buy 123", context.getInput());
                    assertEquals(123, IntegerArgumentType.getInteger(context, "quantity"));
                    assertEquals(2, context.getNodes().size());
                    callCount.incrementAndGet();
                    return 1;
                  })
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertNotForwarded("buy 123");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotForwardedWithImpermissibleNonAliasLevelNode() {
    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("sell")
            .executes(context -> fail())
            .then(RequiredArgumentBuilder
                  .<CommandSource, Integer>argument("quantity", IntegerArgumentType.integer())
                  .executes(context -> fail())
                  .requires(source -> false)
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertNotForwarded("sell 456");
  }

  // Suggestions

  @Test
  void testArgumentSuggestions() {
    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .then(RequiredArgumentBuilder
                .<CommandSource, String>argument("argument", StringArgumentType.word())
                    .suggests((context, builder) ->
                        builder
                          .suggest("foo")
                          .suggest("bar")
                          .suggest("baz")
                          .buildFuture()
                    )
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("hello ", "bar", "baz", "foo");
    assertSuggestions("hello ba", "bar", "baz", "foo");
    assertSuggestions("hello bar", "baz", "foo");
  }

  // The following 2 tests ensure we strictly follow Brigadier's behavior, even if
  // it doesn't make much sense.

  @Test
  void testSuggestionEvenIfImpermissible() {
    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("parent")
            .then(LiteralArgumentBuilder
                    .<CommandSource>literal("child")
                    .requiresWithContext((context, reader) -> fail())
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent ", "child");
    assertSuggestions("parent chi", "child");
  }

  @Test
  void testNoSuggestionIfImpermissible_parse() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("parent")
            .then(LiteralArgumentBuilder
                    .<CommandSource>literal("child")
                    .requiresWithContext((context, reader) -> {
                      // CommandDispatcher#parseNodes checks the child node can be added to
                      // the context object. CommandDispatcher#getCompletionSuggestions then
                      // considers a suggestion context with "parent" as the parent, and
                      // considers the suggestions of relevant children, which includes
                      // "child".
                      assertEquals(2, context.getNodes().size());
                      callCount.incrementAndGet();
                      return false;
                    })
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent child");
    assertEquals(1, callCount.get());
  }
}
