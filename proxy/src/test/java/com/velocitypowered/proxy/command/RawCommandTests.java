package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.velocitypowered.api.command.RawCommand;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class RawCommandTests extends CommandTestSuite {

  // Execution

  @Test
  void testExecuteAlias() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, (RawCommand) invocation -> {
      assertEquals(source, invocation.source());
      assertEquals("hello", invocation.alias());
      assertEquals("", invocation.arguments());
      callCount.incrementAndGet();
    });

    assertHandled("hello");
    assertEquals(1, callCount.get());
  }

  @Test
  void testForwardsAndDoesNotExecuteImpermissibleAlias() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
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
  void testExecutesWithArguments() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("hello").build();
    manager.register(meta, (RawCommand) invocation -> {
      assertEquals("hello", invocation.alias());
      assertEquals("dear world", invocation.arguments());
      callCount.incrementAndGet();
    });

    assertHandled("hello dear world");
    assertEquals(1, callCount.get());
  }

  @Test
  void testHandlesAndDoesNotExecuteWithImpermissibleArgs() {
    final var callCount = new AtomicInteger();

    final var meta = manager.metaBuilder("color").build();
    manager.register(meta, new RawCommand() {
      @Override
      public void execute(final Invocation invocation) {
        fail();
      }

      @Override
      public boolean hasPermission(final Invocation invocation) {
        assertEquals("color", invocation.alias());
        assertEquals("red", invocation.arguments());
        callCount.incrementAndGet();
        return false;
      }
    });

    assertHandled("color red");
    assertEquals(1, callCount.get());
  }
}
