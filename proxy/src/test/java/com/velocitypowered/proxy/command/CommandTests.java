package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class CommandTests extends CommandTestSuite {

  // The following tests don't depend on the Command implementation being used.
  // If adding more tests, try to mix it up by using the different implementations
  // provided by Velocity.

  // Execution

  @Test
  void testAliasIsCaseInsensitive() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("hello", invocation.alias());
    });

    assertNotForwarded("Hello");
  }

  @Test
  void testUnknownAliasIsForwarded() {
    assertForwarded("");
    assertForwarded("foo");
  }

  @Test
  void testExecuteInputIsTrimmed() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertEquals("", invocation.arguments());
      }
    });

    assertNotForwarded(" hello");
    assertNotForwarded("  hello");
    assertNotForwarded("hello ");
    assertNotForwarded("hello   ");
  }

  @Test
  void testExecuteAsyncCompletesExceptionallyIfExecuteThrows() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      throw new RuntimeException("cannot execute");
    });

    try {
      manager.executeAsync(source, "hello").join();
    } catch (final CompletionException e) {
      assertEquals("cannot execute", e.getCause().getCause().getMessage());
    }
  }

  @Test
  void testExecuteThrowsIfHasPermissionThrows() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public boolean hasPermission(final Invocation invocation) {
        throw new RuntimeException("cannot run hasPermission");
      }
    });

    final Exception e = assertThrows(RuntimeException.class, () -> {
      manager.execute(source, "hello");
    });

    assertEquals("cannot run hasPermission", e.getCause().getMessage());
  }

  // Suggestions

  static class DummyCommand implements SimpleCommand {

    @Override
    public void execute(final Invocation invocation) {
      fail();
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
      fail();
      return null;
    }
  }

  @Test
  void testAliasSuggestions() {
    manager.register(manager.metaBuilder("foo").build(), new DummyCommand());
    manager.register(manager.metaBuilder("bar").build(), new DummyCommand());
    manager.register(manager.metaBuilder("baz").build(), new DummyCommand());

    assertSuggestions("", "bar", "baz", "foo"); // in alphabetical order
  }

  @Test
  void testPartialAliasSuggestions() {
    manager.register(manager.metaBuilder("foo").build(), new DummyCommand());
    manager.register(manager.metaBuilder("bar").build(), new DummyCommand());

    assertSuggestions("f", "foo");
  }

  @Test
  void testNoSuggestionsIfFullAlias() {
    manager.register(manager.metaBuilder("hello").build(), new DummyCommand());

    assertSuggestions("hello");
  }

  @Test
  void testNoAliasSuggestionIfImpermissible() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertEquals("", invocation.arguments());
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail();
        return null;
      }
    });

    assertSuggestions("");
    assertSuggestions("hel");
  }

  @Test
  void testOfferSuggestionsCompletesExceptionallyIfSuggestThrows() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        throw new UnsupportedOperationException("cannot suggest");
      }
    });

    try {
      manager.offerSuggestions(source, "hello ").join();
      fail();
    } catch (final CompletionException e) {
      assertEquals("cannot suggest", e.getCause().getMessage());
    }
  }

  // (Secondary) aliases
  // For now, these work as regular literals, but it is possible that they will use Brigadier
  // redirects in the future. The following tests check for inconsistencies between the primary
  // alias node and a secondary alias literal.

  @Test
  void testExecutedWithAlias() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar")
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("bar", invocation.alias());
        assertEquals("", invocation.arguments());
      }
    });

    assertNotForwarded("bar");
  }

  @Test
  void testExecutedWithAliasAndArguments() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar")
            .build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("bar", invocation.alias());
      assertArrayEquals(new String[] { "baz" }, invocation.arguments());
    });

    assertNotForwarded("bar baz");
  }

  @Test
  void testNotExecutedWithImpermissibleAlias() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar")
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("bar", invocation.alias());
        assertEquals("", invocation.arguments());
        return false;
      }
    });

    assertForwarded("bar");
  }

  @Test
  void testNotExecutedWithImpermissibleAliasAndArguments() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar")
            .build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("bar", invocation.alias());
        assertArrayEquals(new String[] { "baz" }, invocation.arguments());
        return false;
      }
    });

    assertNotForwarded("bar baz");
  }

  @Test
  void testAllAliasesAreSuggested() {
    final CommandMeta meta = manager.metaBuilder("foo")
            .aliases("bar", "baz")
            .build();
    manager.register(meta, new DummyCommand());

    assertSuggestions("", "bar", "baz", "foo");
  }

  // Hinting

  @Test
  void testExecuteViaHint() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("people")
            .build();
    final CommandMeta meta = manager.metaBuilder("hello")
            .hint(hint)
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("world", invocation.arguments());
        callCount.incrementAndGet();
      }
    });

    assertNotForwarded("hello world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteViaLiteralChildOfHint() {
    final CommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .then(LiteralArgumentBuilder.literal("baz"))
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals(new String[] { "bar", "baz" }, invocation.arguments());
    });

    assertNotForwarded("foo bar baz");
  }

  @Test
  void testExecuteViaArgumentChildOfHint() {
    // Hints should be wrapped recursively
    final CommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .then(LiteralArgumentBuilder
                    .<CommandSource>literal("baz")
                    .then(RequiredArgumentBuilder
                            .argument("number", IntegerArgumentType.integer())))
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("bar baz 123", invocation.arguments());
      }
    });

    assertNotForwarded("foo bar baz 123");
  }

  @Test
  void testNotExecutedViaHintWithImpermissibleArguments() {
    final CommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("bar", invocation.arguments());
        return false;
      }
    });

    assertNotForwarded("foo bar");
  }

  @Test
  void testSuggestionsAreMergedWithHints() {
    final CommandNode<CommandSource> createHint = LiteralArgumentBuilder
            .<CommandSource>literal("deposit")
            .build();
    final CommandNode<CommandSource> removeHint = LiteralArgumentBuilder
            .<CommandSource>literal("withdraw")
            .build();
    final CommandMeta meta = manager.metaBuilder("bank")
            .hint(createHint)
            .hint(removeHint)
            .build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return ImmutableList.of("balance", "interest");
      }
    });

    assertSuggestions("bank ",
            "balance", "deposit", "interest", "withdraw");
  }

  @Test
  void testNoHintSuggestionsIfImpermissible_empty() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("", invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }
    });

    assertSuggestions("foo ");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNoHintSuggestionsIfImpermissible_partialLiteral() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertArrayEquals(new String[] { "ba" }, invocation.arguments());
        callCount.getAndIncrement();
        return false;
      }
    });

    assertSuggestions("foo ba");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNoHintSuggestionsIfImpermissible_parsedHint() {
    final AtomicInteger callCount = new AtomicInteger();

    final LiteralCommandNode<CommandSource> hint = LiteralArgumentBuilder
            .<CommandSource>literal("bar")
            .build();
    final CommandMeta meta = manager.metaBuilder("foo")
            .hint(hint)
            .build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertArrayEquals(new String[] { "bar" }, invocation.arguments());
        callCount.getAndIncrement();
        return false;
      }
    });

    assertSuggestions("foo bar");
    assertEquals(1, callCount.get());
  }
}
