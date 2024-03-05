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

package com.velocitypowered.proxy.command;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BrigadierCommand}.
 */
public class BrigadierCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testExecutesAlias() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> {
          assertEquals(source, context.getSource());
          assertEquals("hello", context.getInput());
          assertEquals(1, context.getNodes().size());
          callCount.incrementAndGet();
          return 1;
        })
        .build();
    manager.register(new BrigadierCommand(node));

    assertHandled("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteIgnoresAliasCase() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> {
          assertEquals("hello", context.getInput());
          callCount.incrementAndGet();
          return 1;
        })
        .build();
    manager.register(new BrigadierCommand(node));

    assertHandled("Hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteInputIsTrimmed() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> {
          assertEquals("hello", context.getInput());
          callCount.incrementAndGet();
          return 1;
        })
        .build();
    manager.register(new BrigadierCommand(node));

    assertHandled(" hello");
    assertHandled("  hello");
    assertHandled("hello ");
    assertHandled("hello   ");
    assertEquals(4, callCount.get());
  }

  @Test
  void testExecuteAfterUnregisterForwards() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> fail())
        .build();
    manager.register(new BrigadierCommand(node));
    manager.unregister("hello");

    assertForwarded("hello");
  }

  @Test
  void testForwardsAndDoesNotExecuteImpermissibleAlias() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
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
  void testForwardsAndDoesNotExecuteContextImpermissibleAlias() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
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
  void testExecutesNonAliasLevelNode() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("buy")
        .executes(context -> fail())
        .then(RequiredArgumentBuilder
            .<CommandSource, Integer>argument("quantity", integer())
            .executes(context -> {
              assertEquals("buy 12", context.getInput());
              assertEquals(12, getInteger(context, "quantity"));
              assertEquals(2, context.getNodes().size());
              callCount.incrementAndGet();
              return 1;
            }))
        .build();
    manager.register(new BrigadierCommand(node));

    assertHandled("buy 12");
    assertEquals(1, callCount.get());
  }

  @Test
  void testHandlesAndDoesNotExecuteWithImpermissibleNonAliasLevelNode() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> fail())
        .then(LiteralArgumentBuilder
            .<CommandSource>literal("world")
            .executes(context -> fail())
            .requires(source -> {
              callCount.incrementAndGet();
              return false;
            }))
        .build();
    manager.register(new BrigadierCommand(node));

    assertHandled("hello world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteAsyncCompletesExceptionallyOnCallbackException() {
    final var expected = new RuntimeException();
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .executes(context -> {
          throw expected;
        })
        .build();
    manager.register(new BrigadierCommand(node));

    final Exception wrapper = assertThrows(CompletionException.class, () ->
        manager.executeAsync(source, "hello").join());

    assertSame(expected, wrapper.getCause().getCause());
  }

  @Test
  void testExecuteAsyncCompletesExceptionallyOnRequirementException() {
    final var expected = new RuntimeException();
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .requires(source1 -> {
          throw expected;
        })
        .executes(context -> fail()) // needed for dispatcher to consider the node
        .build();
    manager.register(new BrigadierCommand(node));

    final Exception wrapper = assertThrows(CompletionException.class, () ->
        manager.executeAsync(source, "hello").join());

    assertSame(expected, wrapper.getCause().getCause());
  }

  // Suggestions

  @Test
  void testDoesNotSuggestAliasAfterUnregister() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .build();
    manager.register(new BrigadierCommand(node));
    manager.unregister("hello");

    assertSuggestions("");
  }

  @Test
  void testArgumentSuggestions() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("hello")
        .then(RequiredArgumentBuilder
            .<CommandSource, String>argument("argument", word())
            .suggests((context, builder) -> builder
                .suggest("foo")
                .suggest("bar")
                .suggest("baz")
                .buildFuture()))
        .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("hello ", "bar", "baz", "foo");
    assertSuggestions("hello ba", "bar", "baz", "foo");
    assertSuggestions("hello bar", "baz", "foo");
  }

  // The following 2 tests ensure we strictly follow Brigadier's behavior, even
  // if it doesn't make much sense.

  @Test
  void testSuggestsEvenIfImpermissible() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("parent")
        .then(LiteralArgumentBuilder
            .<CommandSource>literal("child")
            .requiresWithContext((context, reader) -> fail()))
        .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent ", "child");
    assertSuggestions("parent chi", "child");
  }

  @Test
  void testDoesNotSuggestIfImpermissibleDuringParse() {
    final var callCount = new AtomicInteger();

    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("parent")
        .then(LiteralArgumentBuilder
            .<CommandSource>literal("child")
            .requiresWithContext((context, reader) -> {
              // CommandDispatcher#parseNodes checks whether the child node can be added
              // to the context object. CommandDispatcher#getCompletionSuggestions then
              // considers a suggestion context with "parent" as the parent, and considers
              // the suggestions of relevant children, which includes "child".
              assertEquals(2, context.getNodes().size());
              callCount.incrementAndGet();
              return false;
            }))
        .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent child");
    assertEquals(1, callCount.get());
  }

  @Test
  void testDoesNotSuggestIfCustomSuggestionProviderFutureCompletesExceptionally() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("parent")
        .then(RequiredArgumentBuilder
            .<CommandSource, String>argument("child", word())
            .suggests((context, builder) ->
                CompletableFuture.failedFuture(new RuntimeException())))
        .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent ");
  }

  @Test
  void testDoesNotSuggestIfCustomSuggestionProviderThrows() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("parent")
        .then(RequiredArgumentBuilder
            .<CommandSource, String>argument("child", word())
            .suggests((context, builder) -> {
              throw new RuntimeException();
            }))
        .build();
    manager.register(new BrigadierCommand(node));

    assertSuggestions("parent ");
  }

  @Test
  void testSuggestCompletesExceptionallyIfRequirementPredicateThrows() {
    final var node = LiteralArgumentBuilder
        .<CommandSource>literal("parent")
        .requires(source1 -> {
          throw new RuntimeException();
        })
        .then(RequiredArgumentBuilder
            .<CommandSource, String>argument("child", word())
            .suggests((context, builder) -> fail()))
        .build();
    manager.register(new BrigadierCommand(node));

    assertThrows(CompletionException.class, () ->
        manager.offerSuggestions(source, "parent ").join());
  }

  @Test
  void testArgumentBuilderCreationUsingStaticFactory() {
    assertDoesNotThrow(() -> BrigadierCommand.literalArgumentBuilder("someCommand"));
    assertThrows(IllegalArgumentException.class,
            () -> BrigadierCommand.literalArgumentBuilder("some random command"));
    assertDoesNotThrow(
            () -> BrigadierCommand.requiredArgumentBuilder(
                    "someRequiredArgument", StringArgumentType.word()));
  }
}
