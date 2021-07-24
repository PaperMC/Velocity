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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class CommandManagerTests extends CommandTestSuite {

  // Registration

  @Test
  void testRegisterWithMeta() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, DummyCommand.INSTANCE);

    assertTrue(manager.hasCommand("hello"));
    assertRegisteredAliases("hello");
  }

  @Test
  void testRegisterWithMetaContainingMultipleAliases() {
    final var meta = manager.metaBuilder("foo")
            .aliases("bar")
            .aliases("baz", "qux")
            .build();
    manager.register(meta, DummyCommand.INSTANCE);

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("baz"));
    assertTrue(manager.hasCommand("qux"));
    assertRegisteredAliases("foo", "bar", "baz", "qux");
  }

  @Test
  void testRegisterAliasesAreCaseInsensitive() {
    final var meta = manager.metaBuilder("Foo")
            .aliases("Bar")
            .build();
    manager.register(meta, DummyCommand.INSTANCE);

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("bar"));
    assertRegisteredAliases("foo", "bar");
  }

  @Test
  void testRegisterBrigadierCommand() {
    final var node = LiteralArgumentBuilder
            .<CommandSource>literal("hello")
            .build();
    manager.register(new BrigadierCommand(node));

    assertTrue(manager.hasCommand("hello"));
    assertRegisteredAliases("hello");
  }

  @Test
  void testRegisterOverridesPreviousCommand() {
    final var called = new AtomicBoolean();

    final var oldMeta = manager.metaBuilder("foo").build();
    manager.register(oldMeta, DummyCommand.INSTANCE); // fails on execution
    final var newMeta = manager.metaBuilder("foo").build();
    manager.register(newMeta, (RawCommand) invocation -> called.set(true));
    manager.executeAsync(MockCommandSource.INSTANCE, "foo").join();

    assertTrue(called.get());
  }

  @Test
  void testAddingExecutableHintToMetaThrows() {
    final var hintNode = LiteralArgumentBuilder
            .<CommandSource>literal("hint")
            .executes(context -> fail())
            .build();

    assertThrows(IllegalArgumentException.class, () -> {
      manager.metaBuilder("hello").hint(hintNode);
    });
  }

  @Test
  void testAddingHintWithRedirectToMetaThrows() {
    final var targetNode = LiteralArgumentBuilder
            .<CommandSource>literal("target")
            .build();
    final var hintNode = LiteralArgumentBuilder
            .<CommandSource>literal("origin")
            .redirect(targetNode)
            .build();

    assertThrows(IllegalArgumentException.class, () -> {
      manager.metaBuilder("hello").hint(hintNode);
    });
  }

  // Un-registration

  @Test
  void testUnregisterUnregisteredAliasIsIgnored() {
    manager.unregister("hello");

    assertFalse(manager.hasCommand("hello"));
    assertRegisteredAliases();
  }

  @Test
  void testUnregisterRegisteredAlias() {
    manager.register("hello", DummyCommand.INSTANCE);
    manager.unregister("hello");

    assertFalse(manager.hasCommand("hello"));
    assertRegisteredAliases();
  }

  @Test
  void testUnregisterSecondaryAlias() {
    final var meta = manager.metaBuilder("foo")
            .aliases("bar")
            .build();
    manager.register(meta, DummyCommand.INSTANCE);
    manager.unregister("bar");

    assertFalse(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("foo"));
    assertRegisteredAliases("foo");
  }

  // Execution

  @Test
  void testExecuteUnknownAliasIsForwarded() {
    assertForwarded("");
    assertForwarded("hello");
  }

  // Suggestions

  @Test
  void testEmptyManagerSuggestNoAliases() {
    assertSuggestions("");
  }

  static final class DummyCommand implements SimpleCommand {

    static final DummyCommand INSTANCE = new DummyCommand();

    private DummyCommand() {}

    @Override
    public void execute(final Invocation invocation) {
      fail();
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
      return fail();
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
      return fail();
    }
  }
}
