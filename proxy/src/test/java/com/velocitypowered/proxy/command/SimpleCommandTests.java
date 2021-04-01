package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class SimpleCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testAliasExecute() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals(source, invocation.source());
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[0], invocation.arguments());
      callCount.incrementAndGet();
    });

    assertNotForwarded("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotExecutedWithImpermissibleAlias() {
    final AtomicInteger callCount = new AtomicInteger();
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals(source, invocation.source());
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[0], invocation.arguments());
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
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[] { "dear", "world" }, invocation.arguments());
      callCount.incrementAndGet();
    });

    assertNotForwarded("hello dear world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotExecutedAndNotForwardedWithImpermissibleArguments() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[] { "world" }, invocation.arguments());
        return false;
      }
    });

    assertNotForwarded("hello world");
  }

  // Suggestions

  @Test
  void testArgumentSuggestionAfterAlias() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[0], invocation.arguments());
        return ImmutableList.of("world", "people");
      }
    });

    assertSuggestions("hello ", "people", "world"); // in alphabetical order
  }

  @Test
  void testArgumentSuggestionsAfterPartialArguments() {
    final CommandMeta meta = manager.metaBuilder("numbers").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertArrayEquals(new String[] { "12345678" }, invocation.arguments());
        return Collections.singletonList("9");
      }
    });

    assertSuggestions("numbers 12345678", "9");
  }

  @Test
  void testNoSuggestionIfSameArguments() {
    final CommandMeta meta = manager.metaBuilder("foo").build();
    manager.register(meta, new SimpleCommand() {
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
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[0], invocation.arguments());
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail();
        return null;
      }
    });

    assertSuggestions("hello ");
  }

  @Test
  void testNoArgumentSuggestionsIfImpermissible() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("foo").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("foo", invocation.alias());
        assertArrayEquals(new String[] { "bar", "baz", "" }, invocation.arguments());
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
}
