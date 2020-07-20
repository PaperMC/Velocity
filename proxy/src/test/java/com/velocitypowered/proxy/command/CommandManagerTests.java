package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
  void testLegacyRegister() {
    VelocityCommandManager manager = createManager();
    LegacyCommand command = new NoopLegacyCommand();

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

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "buy ").join());
    assertTrue(executed.compareAndSet(true, false), "was executed");
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 14").join());
    assertTrue(checkedRequires.compareAndSet(true, false));
    assertTrue(executed.get());
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "buy 9").join(),
            "Invalid arg returns false");
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 9 bananas").join(),
            "no permission returns false");
    assertTrue(checkedRequires.get());
  }

  @Test
  void testLegacyExecute() {
    VelocityCommandManager manager = createManager();
    AtomicBoolean executed = new AtomicBoolean(false);
    LegacyCommand command = invocation -> {
      assertEquals(MockCommandSource.INSTANCE, invocation.source());
      assertArrayEquals(new String[] {"bar", "254"}, invocation.arguments());
      executed.set(true);
    };
    manager.register("foo", command);

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo bar 254").join());
    assertTrue(executed.get());

    LegacyCommand noPermsCommand = new LegacyCommand() {
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
    RawCommand command = invocation -> {
      assertEquals(MockCommandSource.INSTANCE, invocation.source());
      assertEquals("lobby 23", invocation.arguments());
      executed.set(true);
    };
    manager.register("sendMe", command);

    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "sendMe lobby 23").join());
    assertTrue(executed.get());

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
    assertFalse(manager.executeImmediately(MockCommandSource.INSTANCE, "sendThem foo").join());
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
            .build();
    nameNode.addChild(numberNode);
    brigadierNode.addChild(nameNode);
    manager.brigadierBuilder().register(brigadierNode);

    LegacyCommand legacyCommand = new LegacyCommand() {
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
            return fail(Arrays.toString(invocation.arguments()));
        }
      }
    };
    manager.register("legacy", legacyCommand);

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
            return fail(invocation.arguments());
        }
      }
    };
    manager.register("raw", rawCommand);

    assertCollectionsEqual(
            ImmutableList.of("brigadier", "legacy", "raw"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier ")
            .join().isEmpty());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "brigadier foo ")
            .join().isEmpty());
    assertCollectionsEqual(
            ImmutableList.of("foo", "bar"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "legacy ").join());
    assertCollectionsEqual(
            ImmutableList.of("123"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "legAcy foo ").join());
    assertCollectionsEqual(
            ImmutableList.of("foo", "baz"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw ").join());
    assertCollectionsEqual(
            ImmutableList.of("2", "3", "5", "7"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "raw foo ").join());
    assertCollectionsEqual(
            ImmutableList.of("11", "13", "17"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "rAW bar ").join());
  }

  /*@Test
  void testConstruction() {
    VelocityCommandManager manager = createManager();
    assertFalse(manager.hasCommand("foo"));
    assertTrue(manager.getAllRegisteredCommands().isEmpty());
    assertTrue(manager.getBrigadierDispatcher().getRoot().getChildren().isEmpty());
  }

  @Test
  void testLegacyRegistration() {
    final VelocityCommandManager manager = createManager();
    final ALegacyCommand command = new ALegacyCommand();

    manager.register("foo", command);
    assertTrue(manager.hasCommand("foo"));
    assertTrue(manager.getAllRegisteredCommands().contains("foo"));

    manager.unregister("foo");
    assertFalse(manager.hasCommand("foo"));
    assertFalse(manager.getAllRegisteredCommands().contains("foo"));

    // Register with aliases
    manager.register("foo", command, "bar", "baz");
    assertTrue(manager.hasCommand("bar"));
    assertTrue(manager.getAllRegisteredCommands().contains("baz"));
  }

  @Test
  void testLegacy() {
    final VelocityCommandManager manager = createManager();
    final AtomicBoolean executed = new AtomicBoolean(false);
    final LegacyCommand command = new LegacyCommand() {
      @Override
      public void execute(final LegacyCommandInvocation invocation) {
        assertEquals(MockCommandSource.INSTANCE, invocation.source());
        assertEquals("car", invocation.arguments()[0]);
        executed.set(true);
      }

      @Override
      public List<String> suggest(final LegacyCommandInvocation invocation) {
        return ImmutableList.of("bar", "baz");
      }

      @Override
      public boolean hasPermission(final LegacyCommandInvocation invocation) {
        switch (invocation.arguments().length) {
          case 0: // suggestion
            return true;
          case 1:
            return invocation.arguments()[0].equals("bar");
          default:
            return fail();
        }
      }
    };

    manager.register("foo", command);

    manager.offerSuggestions(MockCommandSource.INSTANCE, "")
            .thenAccept(aliases -> {
              // TODO I'm not 100% sure on this one,
              // 1.12 clients need the full suggestion while 1.13+ only needs the completion
              assertTrue(aliases.contains("/foo"));
              assertEquals(1, aliases.size());
            })
            .join();

    manager.offerSuggestions(MockCommandSource.INSTANCE, "foo ")
            .thenAccept(suggestions -> {
              assertTrue(suggestions.contains("bar"));
              assertTrue(suggestions.contains("baz"));
            })
            .join();

    assertTrue(manager.hasPermission(MockCommandSource.INSTANCE, "foo bar"));
    assertFalse(manager.hasPermission(MockCommandSource.INSTANCE, "foo 123"));

    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo bar").join());
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "foo car").join(), "no permission");
    assertTrue(executed.get());
  }

  @Test
  void testBrigadier() {
    final VelocityCommandManager manager = createManager();
    final AtomicBoolean executed = new AtomicBoolean(false);

    CommandNode<CommandSource> node = BrigadierCommand.argumentBuilder("foo")
            // TODO .then(arg)
            .build();

    final BrigadierCommand command = manager.brigadierBuilder().register(node);

    manager.offerSuggestions(MockCommandSource.INSTANCE, "")
            .thenAccept(aliases -> {
              // See TODO on testLegacy
              assertTrue(aliases.contains("/foo"));
              assertEquals(1, aliases.size());
            })
            .join();


  }

  static class ALegacyCommand implements LegacyCommand {
    @Override
    public void execute(final LegacyCommandInvocation invocation) {

    }
  }*/

  static class NoopLegacyCommand implements LegacyCommand {
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
