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

package com.velocitypowered.proxy.connection;

import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhase;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhase;

/**
 * The types of connection that may be selected.
 */
public interface ConnectionType {

  /**
   * The initial {@link ClientConnectionPhase} for this connection type.
   *
   * @return The {@link ClientConnectionPhase}
   */
  ClientConnectionPhase getInitialClientPhase();

  /**
   * The initial {@link BackendConnectionPhase} for this connection type.
   *
   * @return The {@link BackendConnectionPhase}
   */
  BackendConnectionPhase getInitialBackendPhase();

  /**
   * Adds properties to the {@link JavaPlayerIdentity} if required. If any properties
   * are added, the returned {@link JavaPlayerIdentity} will be a copy.
   *
   * @param original The original {@link JavaPlayerIdentity}
   * @param forwardingType The Velocity {@link PlayerInfoForwarding}
   * @return The {@link JavaPlayerIdentity} with the properties added in.
   */
  JavaPlayerIdentity addGameProfileTokensIfRequired(JavaPlayerIdentity original,
                                             PlayerInfoForwarding forwardingType);
}
