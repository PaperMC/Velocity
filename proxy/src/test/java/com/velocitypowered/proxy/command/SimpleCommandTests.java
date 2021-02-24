package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
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
}
