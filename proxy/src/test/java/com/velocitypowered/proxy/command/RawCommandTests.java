package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class RawCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testAliasExecute() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals(source, invocation.source());
        assertEquals("hello", invocation.alias());
        assertEquals("", invocation.arguments());
        callCount.incrementAndGet();
      }
    });

    assertNotForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotExecutedWithImpermissibleAlias() {
    final AtomicInteger callCount = new AtomicInteger();
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals(source, invocation.source());
        assertEquals("hello", invocation.alias());
        assertEquals("", invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }
    });

    assertForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testExecuteWithArguments() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertEquals("dear world", invocation.arguments());
        callCount.incrementAndGet();
      }
    });

    assertNotForwarded("hello dear world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotExecutedAndNotForwardedWithImpermissibleArguments() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertEquals("world", invocation.arguments());
        return false;
      }
    });

    assertNotForwarded("hello world");
  }

  // Suggestions

  @Test
  void testArgumentSuggestionAfterAlias() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertEquals("", invocation.arguments());
        return ImmutableList.of("world", "People", "people");
      }
    });

    // Alphabetical order
    assertSuggestions("hello ", "people", "People", "world");
  }

  @Test
  void testArgumentSuggestionsAfterPartialArguments() {
    final CommandMeta meta = manager.metaBuilder("numbers").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("12345678", invocation.arguments());
        return Collections.singletonList("9");
      }
    });

    assertSuggestions("numbers 12345678", "9");
  }

  @Test
  void testNoSuggestionIfSameArguments() {
    final CommandMeta meta = manager.metaBuilder("foo").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return Collections.singletonList("bar");
      }
    });

    assertSuggestions("foo bar");
  }

  @Test
  void testNoFirstArgumentSuggestionIfImpermissible() {
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
        return null;
      }
    });

    assertSuggestions("hello ");
  }

  @Test
  void testNoArgumentSuggestionsIfImpermissible() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("foo").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("foo", invocation.alias());
        assertEquals("bar baz ", invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail();
        return null;
      }
    });

    assertSuggestions("foo bar baz ");
    assertEquals(1, callCount.get());
  }

  /*@Test
  void testNoHintSuggestionsIfImpermissible() {
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
        if (callCount.getAndIncrement() == 0) {
          assertEquals("ba", invocation.arguments());
        } else {
          assertEquals("", invocation.arguments());
        }
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail();
        return null;
      }
    });

    assertSuggestions("foo ba");
    assertEquals(3, callCount.get());
  }*/
}
