/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandMeta;
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
        return fail();
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
        return fail();
      }
    });

    assertSuggestions("foo bar baz ");
    assertEquals(1, callCount.get());
  }
}
