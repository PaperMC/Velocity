package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class SimpleCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testExecutesAlias() {
    final var callCount = new AtomicInteger();
    
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals(source, invocation.source());
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[0], invocation.arguments());
      callCount.incrementAndGet();
    });
    
    assertHandled("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testForwardsAndDoesNotExecuteImpermissibleAlias() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
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
  void testExecutesWithArguments() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[] { "dear", "world" }, invocation.arguments());
      callCount.incrementAndGet();
    });

    assertHandled("hello dear world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testHandlesAndDoesNotExecuteWithImpermissibleArgs() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("color").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("color", invocation.alias());
        assertArrayEquals(new String[] { "red" }, invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }
    });

    assertHandled("color red");
    assertEquals(1, callCount.get());
  }

  // Suggestions

  @Test
  void testDoesNotSuggestAliasIfImpermissible() {
    final var meta = manager.metaBuilder("hello").build();
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
        return fail();
      }
    });
  }

  @Test
  void testSuggestsArgumentsAfterAlias() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[0], invocation.arguments());
        return ImmutableList.of("world", "people"); // ensures we don't mutate the user's list
      }
    });

    assertSuggestions("hello ", "people", "world"); // in alphabetical order
  }

  @Test
  void testSuggestsArgumentsAfterAliasIgnoresAliasCase() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        return ImmutableList.of("world");
      }
    });

    assertSuggestions("Hello ", "world");
  }

  @Test
  void testSuggestsArgumentsAfterPartialArguments() {
    final var meta = manager.metaBuilder("numbers").build();
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
  void testDoesNotSuggestFirstArgumentIfImpermissibleAlias() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("hello", invocation.alias());
        assertArrayEquals(new String[0], invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return fail();
      }
    });

    assertSuggestions("hello ");
    assertEquals(1, callCount.get());
  }

  @Test
  void testDoesNotSuggestArgumentsAfterPartialImpermissibleArguments() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("foo").build();
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
        return fail();
      }
    });

    assertSuggestions("foo bar baz ");
    assertEquals(1, callCount.get());
  }

  @Test
  void testDoesNotSuggestIfFutureCompletesExceptionally() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFutures.exceptionallyCompletedFuture(new RuntimeException());
      }
    });

    assertSuggestions("hello ");
  }

  @Test
  void testDoesNotSuggestIfSuggestAsyncThrows() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        throw new RuntimeException();
      }
    });

    // Also logs an error to the console, but testing this is quite involved
    assertSuggestions("hello ");
  }

  @Test
  void testSuggestCompletesExceptionallyIfHasPermissionThrows() {
    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        throw new RuntimeException();
      }

      @Override
      public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return fail();
      }
    });

    assertThrows(CompletionException.class, () ->
            manager.offerSuggestions(source, "hello ").join());
  }
}
