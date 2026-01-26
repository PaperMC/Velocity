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
 * Manages the timing and duration of chat messages within the game.
 * The {@code ChatTimeKeeper} class tracks when chat messages are sent and provides mechanisms
 * to determine how long a message has been displayed or to manage message expiration.
 */
public class ChatTimeKeeper {

  private Instant lastTimestamp;

  public ChatTimeKeeper() {
    this.lastTimestamp = Instant.MIN;
  }

  /**
   * Updates the internal timestamp of the chat message or session.
   * This method checks if the provided {@link Instant} is before the current stored timestamp.
   * If it is, the internal timestamp is updated, and the method returns {@code false} to
   * indicate that the update was not successful.
   * If the provided {@link Instant} is valid, the timestamp is updated,
   * and the method may return {@code true}.
   *
   * @param instant the {@link Instant} representing the new timestamp to update
   * @return {@code true} if the timestamp was successfully updated, {@code false}
   *                      if the provided instant is before the current timestamp
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
