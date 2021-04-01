package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

public class LegacyCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testAliasExecute() {
    final AtomicInteger callCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource actualSource, final String @NonNull [] args) {
        assertEquals(source, actualSource);
        assertArrayEquals(new String[0], args);
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
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public boolean hasPermission(final CommandSource actualSource,
                                   final String @NonNull [] args) {
        assertEquals(source, actualSource);
        assertArrayEquals(new String[0], args);
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
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        assertArrayEquals(new String[] { "dear", "world" }, args);
        callCount.incrementAndGet();
      }
    });

    assertNotForwarded("hello dear world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testNotExecutedAndNotForwardedWithImpermissibleArguments() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
        assertArrayEquals(new String[] { "world" }, args);
        return false;
      }
    });

    assertNotForwarded("hello world");
  }

  // Suggestions

  @Test
  void testArgumentSuggestionAfterAlias() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public List<String> suggest(final CommandSource source,
                                  final String @NonNull [] currentArgs) {
        assertArrayEquals(new String[0], currentArgs);
        return ImmutableList.of("world", "People", "people");
      }
    });

    // Alphabetical order
    assertSuggestions("hello ", "people", "People", "world");
  }

  @Test
  void testArgumentSuggestionsAfterPartialArguments() {
    final CommandMeta meta = manager.metaBuilder("numbers").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public List<String> suggest(final CommandSource source,
                                  final String @NonNull [] currentArgs) {
        assertArrayEquals(new String[] { "12345678" }, currentArgs);
        return Collections.singletonList("9");
      }
    });

    assertSuggestions("numbers 12345678", "9");
  }

  @Test
  void testNoSuggestionIfSameArguments() {
    final CommandMeta meta = manager.metaBuilder("foo").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public List<String> suggest(final CommandSource source,
                                  final String @NonNull [] currentArgs) {
        return Collections.singletonList("bar");
      }
    });

    assertSuggestions("foo bar");
  }

  @Test
  void testNoFirstArgumentSuggestionIfImpermissible() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
        assertArrayEquals(new String[0], args);
        return false;
      }

      @Override
      public List<String> suggest(final CommandSource source,
                                  final String @NonNull [] currentArgs) {
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
    manager.register(meta, new Command() {
      @Override
      public void execute(final CommandSource source, final String @NonNull [] args) {
        fail();
      }

      @Override
      public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
        assertArrayEquals(new String[] { "bar", "baz", "" }, args);
        callCount.incrementAndGet();
        return false;
      }

      @Override
      public List<String> suggest(final CommandSource source,
                                  final String @NonNull [] currentArgs) {
        fail();
        return null;
      }
    });

    assertSuggestions("foo bar baz ");
    assertEquals(1, callCount.get());
  }
}
