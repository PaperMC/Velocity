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

package com.velocitypowered.proxy.protocol.packet.chat;

import java.time.Instant;

/**
 * Tracks chat message timestamps to detect out-of-order or future-dated messages.
 */
public class ChatTimeKeeper {

  private Instant lastTimestamp;

  public ChatTimeKeeper() {
    this.lastTimestamp = Instant.MIN;
  }

  /**
   * Records the given timestamp and reports whether it is valid relative to the last one seen.
   *
   * @param instant the timestamp of the latest message
   * @return {@code true} if the timestamp is in order
   */
  public boolean update(Instant instant) {
    if (instant.isBefore(this.lastTimestamp)) {
      this.lastTimestamp = instant;
      return false;
    }
    this.lastTimestamp = instant;
    return true;
  }
}
