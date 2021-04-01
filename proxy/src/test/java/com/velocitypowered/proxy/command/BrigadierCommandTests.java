package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class BrigadierCommandTests extends CommandTestSuite {

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
}
