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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

abstract class CommandTestSuite {

  protected VelocityCommandManager manager;
  protected final CommandSource source = MockCommandSource.INSTANCE;

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(CommandManagerTests.EVENT_MANAGER);
  }

  final void assertNotForwarded(final String cmdLine) {
    assertTrue(manager.executeAsync(source, cmdLine).join());
  }

  final void assertForwarded(final String cmdLine) {
    assertFalse(manager.executeAsync(source, cmdLine).join());
  }

  final void assertSuggestions(final String cmdLine, final String... expectedSuggestions) {
    final List<String> actual = manager.offerSuggestions(source, cmdLine).join();
    final List<String> expectedAsList = Arrays.asList(expectedSuggestions);
    assertEquals(expectedAsList, actual);
  }
}
