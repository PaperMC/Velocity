package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class SimpleCommandTests extends CommandTestSuite {

  // Execution

  class HelloCommand implements SimpleCommand {

    final AtomicInteger callCount = new AtomicInteger();

    @Override
    public void execute(final Invocation invocation) {
      assertEquals(source, invocation.source());
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[0], invocation.arguments());
      this.callCount.incrementAndGet();
    }
  }

  @Test
  void testAliasExecute() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    final HelloCommand command = new HelloCommand();
    manager.register(meta, command);

    assertNotForwarded("hello");
    assertEquals(1, command.callCount.get());
  }

  @Test
  void testAliasIsCaseInsensitive() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new HelloCommand());

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
    manager.register(meta, new HelloCommand());

    assertNotForwarded(" hello");
    assertNotForwarded("  hello");
    assertNotForwarded("hello ");
    assertNotForwarded("hello   ");
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
        return Lists.newArrayList("world", "people");
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
        return Lists.newArrayList("9");
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
  void testNoAliasSuggestionIfImpermissible() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
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
        if (callCount.getAndIncrement() == 0) {
          assertArrayEquals(new String[] { "bar", "baz", "" }, invocation.arguments());
        } else {
          assertArrayEquals(new String[0], invocation.arguments());
        }
        return false;
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail();
        return null;
      }
    });

    assertSuggestions("foo bar baz ");
    // The first call checks the source can use the command with the given arguments.
    // The second verifies we can provide suggestions when no arguments are present.
    assertEquals(2, callCount.get());
  }
}
