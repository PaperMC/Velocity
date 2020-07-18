package com.velocitypowered.proxy.command;

import static com.velocitypowered.proxy.command.CommandManagerTests.createManager;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CommandBuilderTests {

  @Test
  void testBrigadierNoAliases() {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();

    LiteralCommandNode<CommandSource> node = BrigadierCommand
            .argumentBuilder("foo")
            .build();

    BrigadierCommand command = builder.register(node);
    assertNotNull(command);
    assertTrue(manager.getAllRegisteredCommands().contains("foo"));
  }

  @Test
  void testBrigadierAliases() {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();

    LiteralCommandNode<CommandSource> node = BrigadierCommand
            .argumentBuilder("foo")
            .build();

    builder.aliases("bar", "baz").register(node);
    assertTrue(manager.getAllRegisteredCommands().containsAll(
            ImmutableList.of("foo", "bar", "baz")));
  }
}
