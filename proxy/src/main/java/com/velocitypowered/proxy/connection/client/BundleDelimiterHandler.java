/*
 * Copyright (C) 2024 Velocity Contributors
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

import java.util.concurrent.CompletableFuture;

/**
 * BundleDelimiterHandler.
 */
public final class BundleDelimiterHandler {
  private boolean inBundleSession = false;
  private volatile CompletableFuture<Void> finishedBundleSessionFuture;

  public boolean isInBundleSession() {
    return this.inBundleSession;
  }

  /**
   * Toggles the player to be in the process of receiving multiple packets
   * from the backend server via a packet bundle.
   */
  public void toggleBundleSession() {
    if (this.inBundleSession) {
      this.finishedBundleSessionFuture.complete(null);
      this.finishedBundleSessionFuture = null;
    } else {
      this.finishedBundleSessionFuture = new CompletableFuture<>();
    }
    this.inBundleSession = !this.inBundleSession;
  }

  public CompletableFuture<Void> bundleSessionFuture() {
    return this.finishedBundleSessionFuture;
  }
}
