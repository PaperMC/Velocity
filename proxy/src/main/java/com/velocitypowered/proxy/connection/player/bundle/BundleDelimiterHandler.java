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

package com.velocitypowered.proxy.connection.player.bundle;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.BundleDelimiterPacket;
import java.util.concurrent.CompletableFuture;

/**
 * BundleDelimiterHandler.
 */
public final class BundleDelimiterHandler {
  private final ConnectedPlayer player;
  private boolean inBundleSession = false;
  private CompletableFuture<Void> finishedBundleSessionFuture;

  public BundleDelimiterHandler(ConnectedPlayer player) {
    this.player = player;
  }

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

  /**
   * Bundles all packets sent in the given Runnable.
   */
  public CompletableFuture<Void> bundlePackets(final Runnable sendPackets) {
    VelocityServerConnection connectedServer = player.getConnectedServer();
    MinecraftConnection connection = connectedServer == null
        ? null : connectedServer.getConnection();
    if (connection == null) {
      sendPackets(sendPackets);
      return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<Void> future = new CompletableFuture<>();
    connection.eventLoop().execute(() -> {
      if (inBundleSession) {
        finishedBundleSessionFuture.thenRun(() -> {
          sendPackets(sendPackets);
          future.complete(null);
        });
      } else {
        if (connection.getState() == StateRegistry.PLAY) {
          sendPackets(sendPackets);
        } else {
          sendPackets.run();
        }
        future.complete(null);
      }
    });
    return future;
  }

  private void sendPackets(Runnable sendPackets) {
    player.getConnection().write(BundleDelimiterPacket.INSTANCE);
    try {
      sendPackets.run();
    } finally {
      player.getConnection().write(BundleDelimiterPacket.INSTANCE);
    }
  }
}
