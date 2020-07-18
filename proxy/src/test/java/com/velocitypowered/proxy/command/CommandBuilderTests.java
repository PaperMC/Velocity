package com.velocitypowered.proxy.command;

import static com.velocitypowered.proxy.command.CommandManagerTests.createManager;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import org.junit.jupiter.api.Test;

public class CommandBuilderTests {

  @Test
  void testBrigadierNoAliases() throws CommandSyntaxException {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();

    LiteralCommandNode<CommandSource> node = BrigadierCommand
            .argumentBuilder("foo")
            .executes(context -> 36)
            .build();

    BrigadierCommand command = builder.register(node);
    assertNotNull(command);
    assertTrue(manager.getAllRegisteredCommands().contains("foo"));

    ParseResults<CommandSource> parse = manager.getBrigadierDispatcher().parse("foo", null);
    assertFalse(parse.getReader().canRead(), "Node is added to Brigadier dispatcher");
    assertEquals(36, manager.getBrigadierDispatcher().execute(parse));
  }

  @Test
  void testBrigadierAliases() throws CommandSyntaxException {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();

    LiteralCommandNode<CommandSource> node = BrigadierCommand
            .argumentBuilder("foo")
            .executes(context -> 27)
            .build();

    builder.aliases("bar", "baz").register(node);
    assertTrue(manager.getAllRegisteredCommands().containsAll(
            ImmutableList.of("foo", "bar", "baz")));

    ParseResults<CommandSource> parse = manager.getBrigadierDispatcher().parse("bar", null);
    assertFalse(parse.getReader().canRead(), "Alias node is added to Brigadier dispatcher");
    assertEquals(27, manager.getBrigadierDispatcher().execute(parse),
            "Redirect executes command");
  }
}
