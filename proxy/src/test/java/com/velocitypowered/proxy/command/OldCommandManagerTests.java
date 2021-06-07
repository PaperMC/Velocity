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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.event.MockEventManager;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class OldCommandManagerTests {

  static VelocityCommandManager createManager() {
    return new VelocityCommandManager(CommandManagerTests.EVENT_MANAGER);
  }

  @Test
  void testConstruction() {
    VelocityCommandManager manager = createManager();
    assertFalse(manager.hasCommand("foo"));
    assertTrue(manager.getRoot().getChildren().isEmpty());
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "foo").join());
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "bar").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "").join().isEmpty());
  }

  @Test
  void testBrigadierRegister() {
    VelocityCommandManager manager = createManager();
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .build();
    BrigadierCommand command = new BrigadierCommand(node);
    manager.register(command);

    assertEquals(node, command.getNode());
    assertTrue(manager.hasCommand("fOo"));

    LiteralCommandNode<CommandSource> barNode = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    BrigadierCommand aliasesCommand = new BrigadierCommand(barNode);
    CommandMeta meta = manager.createMetaBuilder(aliasesCommand)
            .aliases("baZ")
            .build();

    assertEquals(ImmutableSet.of("bar", "baz"), meta.aliases());
    assertTrue(meta.hints().isEmpty());
    manager.register(meta, aliasesCommand);
    assertTrue(manager.hasCommand("bAr"));
    assertTrue(manager.hasCommand("Baz"));
  }

  @Test
  void testSimpleRegister() {
    VelocityCommandManager manager = createManager();
    SimpleCommand command = new NoopSimpleCommand();

    manager.register("Foo", command);
    assertTrue(manager.hasCommand("foO"));
    manager.unregister("fOo");
    assertFalse(manager.hasCommand("foo"));
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "foo").join());

    manager.register("foo", command, "bAr", "BAZ");
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("bAz"));
  }

  @Test
  void testRawRegister() {
    VelocityCommandManager manager = createManager();
    RawCommand command = new NoopRawCommand();

    manager.register("foO", command, "BAR");
    assertTrue(manager.hasCommand("fOo"));
    assertTrue(manager.hasCommand("bar"));
  }

  @Test
  void testBrigadierExecute() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    AtomicBoolean checkedRequires = new AtomicBoolean(false);
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("buy")
            .executes(context -> {
              assertEquals(MockCommandSource.INSTANCE, context.getSource());
              assertEquals("buy", context.getInput());
              executed.set(true);
              return 1;
            })
            .build();
    CommandNode<CommandSource> quantityNode = RequiredArgumentBuilder
            .<CommandSource, Integer>argument("quantity", IntegerArgumentType.integer(12, 16))
            .requires(source -> {
              assertEquals(MockCommandSource.INSTANCE, source);
              checkedRequires.set(true);
              return true;
            })
            .executes(context -> {
              int argument = IntegerArgumentType.getInteger(context, "quantity");
              assertEquals(14, argument);
              executed.set(true);
              return 1;
            })
            .build();
    CommandNode<CommandSource> productNode = RequiredArgumentBuilder
            .<CommandSource, String>argument("product", StringArgumentType.string())
            .requires(source -> {
              checkedRequires.set(true);
              return false;
            })
            .executes(context -> fail("was executed"))
            .build();
    quantityNode.addChild(productNode);
    node.addChild(quantityNode);
    manager.register(new BrigadierCommand(node));

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "buy ").join());
    assertTrue(executed.compareAndSet(true, false), "was executed");
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 14").join());
    assertTrue(checkedRequires.compareAndSet(true, false));
    assertTrue(executed.get());
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "buy 9").join(),
            "Invalid arg returns false");
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 12 bananas")
        .join());
    assertTrue(checkedRequires.get());
  }

  @Test
  void testSimpleExecute() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    SimpleCommand command = invocation -> {
      assertEquals(MockCommandSource.INSTANCE, invocation.source());
      assertArrayEquals(new String[] {"bar", "254"}, invocation.arguments());
      executed.set(true);
    };
    manager.register("foo", command);

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo bar 254").join());
    assertTrue(executed.get());

    SimpleCommand noPermsCommand = new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail("was executed");
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        return false;
      }
    };

    manager.register("dangerous", noPermsCommand, "veryDangerous");
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "dangerous").join());
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "verydangerous 123")
        .join());
  }

  @Test
  void testRawExecute() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    RawCommand command = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals(MockCommandSource.INSTANCE, invocation.source());
        assertEquals("lobby 23", invocation.arguments());
        executed.set(true);
      }
    };
    manager.register("sendMe", command);

    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "sendMe lobby 23")
        .join());
    assertTrue(executed.compareAndSet(true, false));

    RawCommand noArgsCommand = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("", invocation.arguments());
        executed.set(true);
      }
    };
    manager.register("noargs", noArgsCommand);

    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "noargs").join());
    assertTrue(executed.get());
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "noargs ").join());

    RawCommand noPermsCommand = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail("was executed");
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        return false;
      }
    };

    manager.register("sendThem", noPermsCommand);
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "sendThem foo")
        .join());
  }

  @Test
  void testSuggestions() {
    VelocityCommandManager manager = createManager();

    LiteralCommandNode<CommandSource> brigadierNode = LiteralArgumentBuilder
            .<CommandSource>literal("brigadier")
            .build();
    CommandNode<CommandSource> nameNode = RequiredArgumentBuilder
            .<CommandSource, String>argument("name", StringArgumentType.string())
            .build();
    CommandNode<CommandSource> numberNode = RequiredArgumentBuilder
            .<CommandSource, Integer>argument("quantity", IntegerArgumentType.integer())
            .suggests((context, builder) -> builder.suggest(2).suggest(3).buildFuture())
            .build();
    nameNode.addChild(numberNode);
    brigadierNode.addChild(nameNode);
    manager.register(new BrigadierCommand(brigadierNode));

    SimpleCommand simpleCommand = new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        switch (invocation.arguments().length) {
          case 0:
            return ImmutableList.of("foo", "bar");
          case 1:
            return ImmutableList.of("123");
          default:
            return ImmutableList.of();
        }
      }
    };
    manager.register("simple", simpleCommand);

    RawCommand rawCommand = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        switch (invocation.arguments()) {
          case "":
            return ImmutableList.of("foo", "baz");
          case "foo ":
            return ImmutableList.of("2", "3", "5", "7");
          case "bar ":
            return ImmutableList.of("11", "13", "17");
          default:
            return ImmutableList.of();
        }
      }
    };
    manager.register("raw", rawCommand);

    assertEquals(
            ImmutableList.of("brigadier", "raw", "simple"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "").join(),
            "literals are in alphabetical order");
    assertEquals(
            ImmutableList.of("brigadier"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "briga").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier")
            .join().isEmpty());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier ")
            .join().isEmpty());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier foo")
            .join().isEmpty());
    assertEquals(
            ImmutableList.of("2", "3"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier foo ").join());
    assertEquals(
            ImmutableList.of("bar", "foo"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "simple ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "simple")
            .join().isEmpty());
    assertEquals(
            ImmutableList.of("123"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "simPle foo").join());
    assertEquals(
            ImmutableList.of("baz", "foo"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw ").join());
    assertEquals(
            ImmutableList.of("2", "3", "5", "7"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw foo ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "raw foo")
            .join().isEmpty());
    assertEquals(
            ImmutableList.of("11", "13", "17"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "rAW bar ").join());
  }

  @Test
  void testBrigadierSuggestionPermissions() {
    VelocityCommandManager manager = createManager();
    LiteralCommandNode<CommandSource> manageNode = LiteralArgumentBuilder
            .<CommandSource>literal("manage")
            .requires(source -> false)
            .build();
    CommandNode<CommandSource> idNode = RequiredArgumentBuilder
            .<CommandSource, Integer>argument("id", IntegerArgumentType.integer(0))
            .suggests((context, builder) -> fail("called suggestion builder"))
            .build();
    manageNode.addChild(idNode);
    manager.register(new BrigadierCommand(manageNode));

    // Brigadier doesn't call the children predicate when requesting suggestions.
    // However, it won't query children if the source doesn't pass the parent
    // #requires predicate.
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "manage ")
            .join().isEmpty());
  }

  @Test
  @Disabled
  void testHinting() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    AtomicBoolean calledSuggestionProvider = new AtomicBoolean(false);
    AtomicReference<String> expectedArgs = new AtomicReference<>();
    RawCommand command = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals(expectedArgs.get(), invocation.arguments());
        executed.set(true);
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return ImmutableList.of("raw");
      }
    };

    CommandNode<CommandSource> barHint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .executes(context -> fail("hints don't get executed"))
            .build();
    ArgumentCommandNode<CommandSource, Integer> numberArg = RequiredArgumentBuilder
            .<CommandSource, Integer>argument("number", IntegerArgumentType.integer())
            .suggests((context, builder) -> {
              calledSuggestionProvider.set(true);
              return builder.suggest("456").buildFuture();
            })
            .build();
    barHint.addChild(numberArg);
    CommandNode<CommandSource> bazHint = LiteralArgumentBuilder
            .<CommandSource>literal("baz")
            .build();
    CommandMeta meta = manager.createMetaBuilder("foo")
            .aliases("foo2")
            .hint(barHint)
            .hint(bazHint)
            .build();
    manager.register(meta, command);

    expectedArgs.set("notBarOrBaz");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo notBarOrBaz").join());
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("anotherArg 123");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "Foo2 anotherArg 123").join());
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("bar");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo bar").join());
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("bar 123");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo2 bar 123").join());
    assertTrue(executed.compareAndSet(true, false));

    assertEquals(ImmutableList.of("bar", "baz", "raw"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "foo ").join());
    assertFalse(calledSuggestionProvider.get());
    assertEquals(ImmutableList.of("456"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "foo bar ").join());
    assertTrue(calledSuggestionProvider.compareAndSet(true, false));
    assertEquals(ImmutableList.of(),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "foo2 baz ").join());
  }

  @Test
  void testSuggestionPermissions() throws ExecutionException, InterruptedException {
    VelocityCommandManager manager = createManager();
    RawCommand rawCommand = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail("The Command should not be executed while testing suggestions");
      }

      @Override
      public boolean hasPermission(Invocation invocation) {
        return invocation.arguments().length() > 0;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return ImmutableList.of("suggestion");
      }
    };

    manager.register("foo", rawCommand);

    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "foo").get().isEmpty());
    assertFalse(manager.offerSuggestions(MockCommandSource.INSTANCE, "foo bar").get().isEmpty());

    SimpleCommand oldCommand = new SimpleCommand() {
      @Override
      public void execute(Invocation invocation) {
        fail("The Command should not be executed while testing suggestions");
      }

      @Override
      public boolean hasPermission(Invocation invocation) {
        return invocation.arguments().length > 0;
      }

      @Override
      public List<String> suggest(Invocation invocation) {
        return ImmutableList.of("suggestion");
      }
    };

    manager.register("bar", oldCommand);

    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "bar").get().isEmpty());
    assertFalse(manager.offerSuggestions(MockCommandSource.INSTANCE, "bar foo").get().isEmpty());
  }

  static class NoopSimpleCommand implements SimpleCommand {
    @Override
    public void execute(final Invocation invocation) {

    }
  }

  static class NoopRawCommand implements RawCommand {
    @Override
    public void execute(final Invocation invocation) {

    }
  }
}
