package com.velocitypowered.proxy.command;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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
                    })
            )
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
                    })
            )
            .build();
    manager.register(new BrigadierCommand(node));

    assertHandled("hello world");
    assertEquals(1, callCount.get());
  }
}
