package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.velocitypowered.api.command.SimpleCommand;
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
}
