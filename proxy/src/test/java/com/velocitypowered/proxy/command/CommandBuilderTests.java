package com.velocitypowered.proxy.command;

import static com.velocitypowered.proxy.command.CommandManagerTests.createManager;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import org.junit.jupiter.api.Test;

public class CommandBuilderTests {

  @Test
  void testBrigadierNoAliases() {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .executes(context -> 1)
            .build();
    BrigadierCommand command = builder.register(node);
    assertNotNull(command);
    assertTrue(manager.hasCommand("Foo"));
  }

  @Test
  void testBrigadierWithAliases() {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .executes(context -> 1)
            .build();
    BrigadierCommand command = builder
            .aliases("bar", "bAZ")
            .aliases("Cool", "wow")
            .register(node);
    assertNotNull(command);
    assertTrue(manager.hasCommand("Foo"));
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("cool"));
  }
}
