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

import com.mojang.brigadier.CommandDispatcher;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandGraphInjectorTests {

  private CommandDispatcher<Object> dispatcher;
  private Lock lock;
  private CommandGraphInjector<Object> injector;

  @BeforeEach
  void setUp() {
    this.dispatcher = new CommandDispatcher<>();
    this.lock = new ReentrantLock();
    this.injector = new CommandGraphInjector<>(this.dispatcher, this.lock);
  }

  // TODO

  @Test
  void testInjectInvocableCommand() {
    // Preserves arguments node and hints
  }

  @Test
  void testFiltersImpermissibleAlias() {

  }

  @Test
  void testInjectsBrigadierCommand() {

  }

  @Test
  void testFiltersImpermissibleBrigadierCommandChildren() {

  }

  @Test
  void testInjectFiltersBrigadierCommandRedirects() {

  }

  @Test
  void testInjectOverridesAliasInDestination() {

  }
}
