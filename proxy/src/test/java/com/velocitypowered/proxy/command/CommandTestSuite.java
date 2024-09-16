/*
 * Copyright (C) 2021 Velocity Contributors
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.event.MockEventManager;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.util.Arrays;
import java.util.Collection;
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

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(eventManager, new FakePluginManager());
  }

  final void assertHandled(final String input) {
    assertTrue(manager.executeAsync(source, input).join());
  }

  final void assertForwarded(final String input) {
    assertFalse(manager.executeAsync(source, input).join());
  }

  final void assertSuggestions(final String input, final String... expectedSuggestions) {
    final var actual = manager.offerSuggestions(source, input).join();
    assertEquals(Arrays.asList(expectedSuggestions), actual);
  }

  final void assertPlayerSuggestions(final String input, final String... expectedSuggestions) {
    final var player = mock(Player.class);
    when(player.getPermissionValue(any())).thenReturn(Tristate.UNDEFINED);
    final var actual = manager.offerSuggestions(player, input).join();
    assertEquals(Arrays.asList(expectedSuggestions), actual);
  }

  final void assertRegisteredAliases(final String... expected) {
    final Collection<String> actual = manager.getAliases();
    assertEquals(expected.length, actual.size());
    final Collection<String> asList = Arrays.asList(expected);
    assertTrue(asList.containsAll(actual));
    assertTrue(actual.containsAll(asList));
  }
}
