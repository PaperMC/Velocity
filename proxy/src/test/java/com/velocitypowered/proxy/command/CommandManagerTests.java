package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

public class CommandManagerTests {

  private static final VelocityEventManager EVENT_MANAGER = new MockEventManager();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        EVENT_MANAGER.shutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));
  }

  static VelocityCommandManager createManager() {
    return new VelocityCommandManager(EVENT_MANAGER);
  }

  private static <T> void assertCollectionsEqual(
          final Collection<T> expected, final Collection<T> actual) {
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void testConstruction() {
    VelocityCommandManager manager = createManager();
    assertFalse(manager.hasCommand("foo"));
    assertTrue(manager.getDispatcher().getRoot().getChildren().isEmpty());
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "foo"));
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "bar"));
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "").join().isEmpty());
  }

  @Test
  void testBrigadierRegister() {
    VelocityCommandManager manager = createManager();
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .build();
    manager.brigadierBuilder()
            .aliases("bAR", "BAZ")
            .register(node);

    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.hasCommand("Baz"));
  }

  @Test
  void testBrigadierSelfAliasThrows() {
    VelocityCommandManager manager = createManager();
    BrigadierCommand.Builder builder = manager.brigadierBuilder();
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .executes(context -> 1)
            .build();
    assertThrows(IllegalArgumentException.class, () -> builder.aliases("fOO").register(node));
  }

  @Test
  void testSimpleRegister() {
    VelocityCommandManager manager = createManager();
    SimpleCommand command = new NoopSimpleCommand();

    manager.register("Foo", command);
    assertTrue(manager.hasCommand("foO"));
    manager.unregister("fOo");
    assertFalse(manager.hasCommand("foo"));

    manager.register("foo", command, "bAr", "BAZ");
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.hasCommand("bAz"));
  }

  @Test
  void testRawRegister() {
    VelocityCommandManager manager = createManager();
    RawCommand command = new NoopRawCommand();

    assertThrows(IllegalArgumentException.class, () -> manager.register(command),
            "no aliases throws");
    manager.register(command, "foO", "BAR");
    assertTrue(manager.hasCommand("fOo"));
    assertTrue(manager.hasCommand("bar"));
  }

  @Test
  void testDeprecatedRegister() {
    VelocityCommandManager manager = createManager();
    Command command = new NoopDeprecatedCommand();

    manager.register("foo", command);
    assertTrue(manager.hasCommand("foO"));
  }

  @Test
  void testAlreadyRegisteredThrows() {
    VelocityCommandManager manager = createManager();
    manager.register("bar", new NoopDeprecatedCommand());
    assertThrows(IllegalArgumentException.class, () ->
            manager.register("BAR", new NoopSimpleCommand()));
    assertThrows(IllegalArgumentException.class, () ->
            manager.register("baz", new NoopSimpleCommand(), "bAZ"));
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
    manager.brigadierBuilder().register(node);

    assertTrue(manager.executeAsync(MockCommandSource.INSTANCE, "buy ").join());
    assertTrue(executed.compareAndSet(true, false), "was executed");
    assertTrue(manager.executeImmediatelyAsync(MockCommandSource.INSTANCE, "buy 14").join());
    assertTrue(checkedRequires.compareAndSet(true, false));
    assertTrue(executed.get());
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "buy 9"),
            "Invalid arg returns false");
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 12 bananas"));
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

    assertTrue(manager.executeAsync(MockCommandSource.INSTANCE, "foo bar 254").join());
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
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "dangerous"));
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "verydangerous 123"));
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

    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "sendMe lobby 23"));
    assertTrue(executed.compareAndSet(true, false));

    RawCommand noArgsCommand = new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("", invocation.arguments());
        executed.set(true);
      }
    };
    manager.register("noargs", noArgsCommand);

    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "noargs"));
    assertTrue(executed.get());
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "noargs "));

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
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "sendThem foo"));
  }

  @Test
  void testDeprecatedExecute() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    Command command = new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        assertEquals(MockCommandSource.INSTANCE, source);
        assertArrayEquals(new String[] { "boo", "123" }, args);
        executed.set(true);
      }
    };
    manager.register("foo", command);

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo boo 123"));
    assertTrue(executed.get());

    Command noPermsCommand = new Command() {
      @Override
      public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
        return false;
      }
    };

    manager.register("oof", noPermsCommand, "veryOof");
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "veryOOF"));
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "ooF boo 54321"));
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
    manager.brigadierBuilder().register(brigadierNode);

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

    Command deprecatedCommand = new Command() {
      @Override
      public List<String> suggest(
              final CommandSource source, final String @NonNull [] currentArgs) {
        switch (currentArgs.length) {
          case 0:
            return ImmutableList.of("boo", "scary");
          case 1:
            return ImmutableList.of("123", "456");
          default:
            return ImmutableList.of();
        }
      }
    };
    manager.register("deprecated", deprecatedCommand);

    assertCollectionsEqual(
            ImmutableList.of("brigadier", "simple", "raw", "deprecated"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "").join());
    assertCollectionsEqual(
            ImmutableList.of("brigadier"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "briga").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier")
            .join().isEmpty());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier ")
            .join().isEmpty());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier foo")
            .join().isEmpty());
    assertCollectionsEqual(
            ImmutableList.of("2", "3"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier foo ").join());
    assertCollectionsEqual(
            ImmutableList.of("foo", "bar"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "simple ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "simple")
            .join().isEmpty());
    assertCollectionsEqual(
            ImmutableList.of("123"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "simPle foo ").join());
    assertCollectionsEqual(
            ImmutableList.of("foo", "baz"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw ").join());
    assertCollectionsEqual(
            ImmutableList.of("2", "3", "5", "7"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw foo ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "raw foo")
            .join().isEmpty());
    assertCollectionsEqual(
            ImmutableList.of("11", "13", "17"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "rAW bar ").join());
    assertCollectionsEqual(
            ImmutableList.of("boo", "scary"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "deprecated ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "deprecated")
            .join().isEmpty());
    assertCollectionsEqual(
            ImmutableList.of("123", "456"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "deprEcated foo ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "deprecated foo 789 ")
            .join().isEmpty());
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
    manager.brigadierBuilder().register(manageNode);

    // Brigadier doesn't call the children predicate when requesting suggestions.
    // However, it won't query children if the source doesn't pass the parent
    // #requires predicate.
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "manage ")
            .join().isEmpty());
  }

  @Test
  void testBrigadierPermissionPredicate() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean checkedPermission = new AtomicBoolean(false);
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal("foo")
            .executes(context -> fail())
            .build();
    CommandNode<CommandSource> args = RequiredArgumentBuilder
            .<CommandSource, Integer>argument("bars", IntegerArgumentType.integer())
            .executes(context -> fail())
            .build();
    node.addChild(args);
    manager.brigadierBuilder()
            .permission(context -> {
              assertEquals(MockCommandSource.INSTANCE, context.getSource());
              checkedPermission.set(true);
              return false;
            })
            .aliases("baz")
            .register(node);

    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "foo"));
    assertTrue(checkedPermission.compareAndSet(true, false));
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "baz"));
    assertTrue(checkedPermission.get());
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

  static class NoopDeprecatedCommand implements Command {
    @Override
    public void execute(final CommandSource source, final String @NonNull [] args) {

    }
  }
}
