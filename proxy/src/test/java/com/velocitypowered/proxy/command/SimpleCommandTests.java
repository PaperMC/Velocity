package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class SimpleCommandTests extends CommandTestSuite {

  @Test
  void testExecuteWithNoArguments() {
    final AtomicInteger execCount = new AtomicInteger();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals(dummySource, invocation.source());
      assertEquals("hello", invocation.alias());
      assertArrayEquals(new String[0], invocation.arguments());
      execCount.incrementAndGet();
    });

    assertExecuted("hello");
    assertExecuted("Hello"); // aliases are case-insensitive
    assertExecuted("helLO");
    assertNotExecuted("");
    assertNotExecuted("hell");
    assertNotExecuted("hèlló");

    assertNotExecuted(" hello"); // ignore leading whitespace
    assertNotExecuted("  hello");
    assertExecuted("hello "); // ignore trailing whitespace
    assertExecuted("hello   ");

    assertEquals(5, execCount.get());
  }

  @Test
  void testExecuteWithWordArgument() {
    final AtomicInteger execCount = new AtomicInteger();
    final AtomicReference<String[]> expectedArgs = new AtomicReference<>();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("hello", invocation.alias());
      assertArrayEquals(expectedArgs.get(), invocation.arguments());
      execCount.incrementAndGet();
    });

    expectedArgs.set(new String[] { "world" });
    assertExecuted("hello world");
    assertExecuted("hello world ");
    assertExecuted("hello world  ");

    expectedArgs.set(new String[] { "World!" }); // arguments are case-sensitive
    assertExecuted("Hello World!");
    assertExecuted("hello World!    "); // ignore trailing whitespace

    expectedArgs.set(new String[]{ "", "world" }); // parse leading whitespace
    assertExecuted("hello  world");
    assertExecuted("hello  world  ");

    expectedArgs.set(new String[]{ "", "", "", "Mundo" });
    assertExecuted("HELLO    Mundo");
    assertExecuted("hello    Mundo ");

    assertNotExecuted("hell world");
    assertNotExecuted("helloworld");

    assertEquals(9, execCount.get());
  }

  @Test
  void testExecuteWithArgumentsString() {
    final AtomicInteger execCount = new AtomicInteger();
    final AtomicReference<String[]> expectedArgs = new AtomicReference<>();

    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, (SimpleCommand) invocation -> {
      assertEquals("hello", invocation.alias());
      assertArrayEquals(expectedArgs.get(), invocation.arguments());
      execCount.incrementAndGet();
    });

    expectedArgs.set(new String[]{ "beautiful", "world" });
    assertExecuted("hello beautiful world");
    assertExecuted("hello beautiful world  ");

    expectedArgs.set(new String[]{ "beautiful", "", "", "world" });
    assertExecuted("hello beautiful   world");
    assertExecuted("hello beautiful   world ");

    expectedArgs.set(new String[]{ "This", "", "is", "", "", "a", "sentence" });
    assertExecuted("hello this  is   a sentence");

    assertEquals(5, execCount.get());
  }

  @Test
  void testAliasSuggestions() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        fail("Alias suggestion must not call command for suggestions");
        return null;
      }
    });

    // TODO Isn't this testing CommandManager instead of SimpleCommand?
    assertSuggestions("", "hello");
    assertSuggestions("hel", "hello");
    assertSuggestions("HE", "hello");

    assertSuggestions("hello"); // once complete, no suggestions
    assertSuggestions("a");
  }

  final class WorldSuggestionCommand implements SimpleCommand {

    final AtomicInteger callCount = new AtomicInteger();
    final AtomicReference<String[]> expectedArgs = new AtomicReference<>();

    @Override
    public void execute(final Invocation invocation) {
      fail();
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
      assertEquals(dummySource, invocation.source());
      assertEquals("hello", invocation.alias());
      assertArrayEquals(expectedArgs.get(), invocation.arguments());
      return Collections.singletonList("world");
    }
  }

  @Test
  void testArgumentSuggestionsAfterAlias() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    final WorldSuggestionCommand command = new WorldSuggestionCommand();
    manager.register(meta, command);

    command.expectedArgs.set(new String[0]);
    assertSuggestions("hello ", "world");
    assertSuggestions("Hello ", "world");

    command.expectedArgs.set(new String[] { "cyber" });
    assertSuggestions("hello cyber", "world");

    command.expectedArgs.set(new String[] { "world" });
    assertSuggestions("hello world"); // exact match, no suggestion

    command.expectedArgs.set(new String[] { "World" });
    assertSuggestions("hello World", "world"); // case sensitive

    assertEquals(5, command.callCount.get());
  }

  @Test
  void testArgumentSuggestionsWithinArguments() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    final WorldSuggestionCommand command = new WorldSuggestionCommand();
    manager.register(meta, command);

    // Unlike execute, the suggest method receives the non-trimmed command line
    command.expectedArgs.set(new String[]{ "wonderful", "" });
    assertSuggestions("hello wonderful ", "world");

    command.expectedArgs.set(new String[]{ "dear", "", "" });
    assertSuggestions("hello dear  ", "world");

    command.expectedArgs.set(new String[]{ "dear", "world" }); // Only single-arg can match exactly
    assertSuggestions("hello dear world", "world");

    command.expectedArgs.set(new String[]{ "Dear", "wor" });
    assertSuggestions("hello Dear wor", "world");

    command.expectedArgs.set(new String[]{ "I'm", "", "", "in", "the", "", "", "" });
    assertSuggestions("hello I'm   in the   ", "world");
  }

  @Test
  void testStringArgumentSuggestion() {
    final AtomicReference<String[]> expectedArgs = new AtomicReference<>();

    final CommandMeta meta = manager.metaBuilder("This").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        assertEquals("this", invocation.alias());
        assertArrayEquals(expectedArgs.get(), invocation.arguments());
        return Arrays.asList("is a sentence", "is another sentence");
      }
    });

    expectedArgs.set(new String[0]);
    assertSuggestions("This ", "is a sentence", "is another sentence");

    expectedArgs.set(new String[]{ "is", "" });
    assertSuggestions("This is ", "is a sentence", "is another sentence");

    expectedArgs.set(new String[] { "is", "a" });
    assertSuggestions("This is a", "is a sentence", "is another sentence");

    // Exact match
    expectedArgs.set(new String[]{ "is", "a", "sentence" });
    assertSuggestions("This is a sentence", "is another sentence");

    // Other exact match
    expectedArgs.set(new String[]{ "is", "another", "sentence" });
    assertSuggestions("This is another sentence", "is a sentence");
  }

  @Test
  void testArgumentSuggestionsAreSortedAlphabetically() {
    final CommandMeta meta = manager.metaBuilder("hello").build();
    manager.register(meta, new SimpleCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public List<String> suggest(final Invocation invocation) {
        return Arrays.asList("world", "people", "World"); // unordered intentionally
      }
    });

    assertSuggestions("hello ", "people", "World", "world");
  }
}
