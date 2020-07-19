package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.api.command.LegacyCommandInvocation;
import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
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

  @Test
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
  }
}
