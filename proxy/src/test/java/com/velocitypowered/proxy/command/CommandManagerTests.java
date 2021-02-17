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
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    BrigadierCommand command = new BrigadierCommand(node);
    manager.register(command);

    assertEquals(node, command.getNode());
    assertTrue(manager.hasCommand("fOo"));

    LiteralCommandNode<CommandSource> barNode = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    BrigadierCommand aliasesCommand = new BrigadierCommand(barNode);
    CommandMeta meta = manager.metaBuilder(aliasesCommand)
            .aliases("baZ")
            .build();

    assertEquals(ImmutableSet.of("bar", "baz"), meta.getAliases());
    assertTrue(meta.getHints().isEmpty());
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
    assertFalse(manager.execute(MockCommandSource.INSTANCE, "foo"));

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

    assertTrue(manager.executeAsync(MockCommandSource.INSTANCE, "buy ").join());
    assertTrue(executed.compareAndSet(true, false), "was executed");
    assertTrue(manager.executeImmediatelyAsync(MockCommandSource.INSTANCE, "buy 14").join());
    assertTrue(checkedRequires.compareAndSet(true, false));
    assertTrue(executed.get());
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "buy 9"),
            "Invalid arg returns false");
    assertTrue(manager.executeImmediately(MockCommandSource.INSTANCE, "buy 12 bananas"));
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

    assertEquals(
            ImmutableList.of("brigadier", "deprecated", "raw", "simple"),
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
    assertEquals(
            ImmutableList.of("boo", "scary"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "deprecated ").join());
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "deprecated")
            .join().isEmpty());
    assertEquals(
            ImmutableList.of("123", "456"),
            manager.offerSuggestions(MockCommandSource.INSTANCE, "deprEcated foo").join());
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
    manager.register(new BrigadierCommand(manageNode));

    // Brigadier doesn't call the children predicate when requesting suggestions.
    // However, it won't query children if the source doesn't pass the parent
    // #requires predicate.
    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "manage ")
            .join().isEmpty());
  }

  @Test
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
    CommandMeta meta = manager.metaBuilder("foo")
            .aliases("foo2")
            .hint(barHint)
            .hint(bazHint)
            .build();
    manager.register(meta, command);

    expectedArgs.set("notBarOrBaz");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo notBarOrBaz"));
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("anotherArg 123");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "Foo2 anotherArg 123"));
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("bar");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo bar"));
    assertTrue(executed.compareAndSet(true, false));
    expectedArgs.set("bar 123");
    assertTrue(manager.execute(MockCommandSource.INSTANCE, "foo2 bar 123"));
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

    manager.register(rawCommand, "foo");

    assertTrue(manager.offerSuggestions(MockCommandSource.INSTANCE, "foo").get().isEmpty());
    assertFalse(manager.offerSuggestions(MockCommandSource.INSTANCE, "foo bar").get().isEmpty());

    Command oldCommand = new Command() {
      @Override
      public void execute(CommandSource source, String @NonNull [] args) {
        fail("The Command should not be executed while testing suggestions");
      }

      @Override
      public boolean hasPermission(CommandSource source, String @NonNull [] args) {
        return args.length > 0;
      }

      @Override
      public List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
        return ImmutableList.of("suggestion");
      }
    };

    manager.register(oldCommand, "bar");

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

  static class NoopDeprecatedCommand implements Command {
    @Override
    public void execute(final CommandSource source, final String @NonNull [] args) {

    }
  }
}
