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

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandManagerTests {

  static final VelocityEventManager EVENT_MANAGER = new MockEventManager();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        EVENT_MANAGER.shutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));
  }

  private VelocityCommandManager manager;

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(EVENT_MANAGER);
  }

  // TODO Move execution, suggestion and permission tests to their own classes

  // Registration

  @Test
  void testRegisterWithMeta() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new DummyCommand());

    assertTrue(manager.hasCommand("hello"));
  }

  @Test
  void testRegisterWithMetaContainingMultipleAliases() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar")
            .aliases("baz", "idk")
            .build();
    manager.register(meta, new DummyCommand());

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("baz"));
    assertTrue(manager.hasCommand("idk"));
  }

  @Test
  void testRegisterWithSingleAlias() {
    manager.register("hello", new DummyCommand());

    assertTrue(manager.hasCommand("hello"));
  }

  @Test
  void testRegisterWithAliases() {
    manager.register("foo", new DummyCommand(), "bar", "baz");

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("baz"));
  }

  @Test
  void testRegisterWithNoAliasesThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      manager.register(new DummyCommand());
    });
  }

  @Test
  void testRegisterMetaAliasesAreCaseInsensitive() {
    final CommandMeta meta = manager.metaBuilder("Foo").aliases("Bar").build();
    manager.register(meta, new DummyCommand());

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
  }

  @Test
  void testRegisterAliasesAreCaseInsensitive() {
    manager.register("FOO", new DummyCommand(), "Bar");

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
  }

  @Test
  void testBrigadierRegister() {
    LiteralCommandNode<CommandSource> literal = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .build();
    manager.register(new BrigadierCommand(literal));

    assertTrue(manager.hasCommand("hello"));
  }

  @Test
  void testRegisterOverridesPreviousCommand() {
    final AtomicBoolean called = new AtomicBoolean(false);

    manager.register("foo", new DummyCommand());
    manager.register("foo", (SimpleCommand) invocation -> called.set(true));
    manager.execute(MockCommandSource.INSTANCE, "foo");

    assertTrue(called.get());
  }

  @Test
  void testRegisterWithExecutableHintThrows() {
    final LiteralCommandNode<CommandSource> hintNode = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .executes(context -> fail())
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hintNode)
            .build();

    assertThrows(IllegalArgumentException.class, () -> {
      manager.register(meta, new DummyCommand());
    });
  }

  @Test
  void testRegisterWithRedirectHintThrows() {
    final LiteralCommandNode<CommandSource> targetNode = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final LiteralCommandNode<CommandSource> hintNode = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .redirect(targetNode)
            .build();
    final CommandMeta meta = manager.metaBuilder("foo").hint(hintNode).build();

    assertThrows(IllegalArgumentException.class, () -> {
      manager.register(meta, new DummyCommand());
    });
  }

  // Un-registration

  @Test
  void testUnregister() {
    manager.register("foo", new DummyCommand());
    manager.unregister("foo");

    assertFalse(manager.hasCommand("foo"));
  }

  @Test
  void testUnregisterAlias() {
    final CommandMeta meta = manager.metaBuilder("foo").aliases("bar").build();
    manager.register(meta, new DummyCommand());
    manager.unregister("bar");

    assertTrue(manager.hasCommand("foo"));
    assertFalse(manager.hasCommand("bar"));
  }

  static class DummyCommand implements SimpleCommand {
    @Override
    public void execute(final Invocation invocation) {
      fail();
    }
  }
}
