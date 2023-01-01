/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.client;

/**
 * The vanilla {@link ClientConnectionPhase}s.
 *
 * <p>See {@link com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeHandshakeClientPhase}
 * for Legacy Forge phases</p>
 */
public final class ClientConnectionPhases {

  /**
   * The client is connecting with a vanilla client (as far as we can tell).
   */
  public static final ClientConnectionPhase VANILLA = new ClientConnectionPhase() {
  };

  private ClientConnectionPhases() {
    throw new AssertionError();
  }
}
