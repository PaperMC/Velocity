/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.player.bossbar;

import com.velocitypowered.proxy.adventure.VelocityBossBarImplementation;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles dropping and resending boss bar packets on versions 1.20.2 and newer because the client now
 * deletes all boss bars during the login phase, and sending update packets would cause the client to be disconnected.
 */
public final class BossBarManager {

  private final ConnectedPlayer player;
  private final Set<VelocityBossBarImplementation> bossBars = new HashSet<>();

  private boolean dropPackets = false;

  public BossBarManager(ConnectedPlayer player) {
    this.player = player;
  }

  /**
   * Records the specified boss bar to be re-sent when a player changes server, and sends the update packet
   * if the client is able to receive it and not be disconnected.
   */
  public synchronized void writeUpdate(VelocityBossBarImplementation bar, BossBarPacket packet) {
    this.bossBars.add(bar);
    if (!this.dropPackets) {
      this.player.getConnection().write(packet);
    }
  }

  /**
   * Removes the specified boss bar from the player to ensure it is not re-sent.
   */
  public synchronized void remove(VelocityBossBarImplementation bar, BossBarPacket packet) {
    this.bossBars.remove(bar);
    if (!this.dropPackets) {
      this.player.getConnection().write(packet);
    }
  }

  /**
   * Re-creates the boss bars the player can see with any updates that may have occurred in the meantime,
   * and allows update packets for those boss bars to be sent.
   */
  public synchronized void sendBossBars() {
    for (VelocityBossBarImplementation bossBar : bossBars) {
      bossBar.createDirect(player);
    }
    this.dropPackets = false;
  }

  /**
   * Prevents the player from receiving boss bar update packets while logging in to a new server.
   */
  public synchronized void dropPackets() {
    this.dropPackets = true;
  }
}
