package com.velocitypowered.proxy.command;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

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

  // Suggestions

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
}
