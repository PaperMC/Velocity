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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.event.MockEventManager;
import com.velocitypowered.proxy.event.VelocityEventManager;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

abstract class CommandTestSuite {

  private static VelocityEventManager eventManager;

  protected VelocityCommandManager manager;
  protected final CommandSource source = MockCommandSource.INSTANCE;

  @BeforeAll
  static void beforeAll() {
    eventManager = new MockEventManager();
  }

  @AfterAll
  static void afterAll() {
    try {
      eventManager.shutdown();
      eventManager = null;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(eventManager);
  }

  final void assertHandled(final String input) {
    assertTrue(manager.execute(source, input).join());
  }

  final void assertForwarded(final String input) {
    assertFalse(manager.execute(source, input).join());
  }

  final void assertSuggestions(final String input, final String... expectedSuggestions) {
    final var actual = manager.offerSuggestions(source, input).join();
    assertEquals(Arrays.asList(expectedSuggestions), actual);
  }
}
