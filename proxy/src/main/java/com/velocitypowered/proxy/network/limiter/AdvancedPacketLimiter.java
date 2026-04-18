/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.network.limiter;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.util.IntervalledCounter;
import java.util.HashMap;
import java.util.Map;

/**
 * An advanced packet limiter that can enforce limits on specific packet types.
 */
public final class AdvancedPacketLimiter {
  private final Map<Class<? extends MinecraftPacket>, PacketLimit> limits = new HashMap<>();
  private final long windowNanos;

  /**
   * Creates a new {@code AdvancedPacketLimiter} with the specified window.
   *
   * @param windowNanos the window in nanoseconds
   */
  public AdvancedPacketLimiter(long windowNanos) {
    this.windowNanos = windowNanos;
    // Default strict limits for commonly abused packets
    setupDefaultLimits();
  }

  /**
   * Sets up default packet limits for commonly abused packet types.
   */
  private void setupDefaultLimits() {
    // Max 10 tab completes per second
    limits.put(TabCompleteRequestPacket.class, new PacketLimit(10));
    // Max 20 chat/command packets per second
    limits.put(LegacyChatPacket.class, new PacketLimit(20));
    limits.put(SessionPlayerChatPacket.class, new PacketLimit(20));
    // Max 5 plugin messages per second (non-essential ones)
    limits.put(PluginMessagePacket.class, new PacketLimit(5));
    // Max 2 client settings updates per second
    limits.put(ClientSettingsPacket.class, new PacketLimit(2));
  }

  /**
   * Accounts for a packet and returns whether it should be allowed.
   *
   * @param packet the packet to account for
   * @return true if allowed, false if rate limited
   */
  public boolean account(MinecraftPacket packet) {
    PacketLimit limit = limits.get(packet.getClass());
    if (limit == null) {
      return true;
    }
    return limit.account(System.nanoTime(), windowNanos);
  }

  /**
   * Represents a rate limit for a specific packet type.
   */
  private static class PacketLimit {
    private final int maxPerSecond;
    private final IntervalledCounter counter;

    /**
     * Creates a new {@code PacketLimit} with the specified maximum rate.
     *
     * @param maxPerSecond the maximum packets allowed per second
     */
    PacketLimit(int maxPerSecond) {
      this.maxPerSecond = maxPerSecond;
      this.counter = new IntervalledCounter(1_000_000_000L); // 1 second window for rate calculation
    }

    /**
     * Accounts for a packet and returns whether it should be allowed.
     *
     * @param now the current time in nanoseconds
     * @param windowNanos the window in nanoseconds
     * @return true if allowed, false if rate limited
     */
    boolean account(long now, long windowNanos) {
      counter.updateAndAdd(1, now);
      return counter.getRate() <= maxPerSecond;
    }
  }
}
