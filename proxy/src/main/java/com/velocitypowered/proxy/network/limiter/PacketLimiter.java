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

/**
 * PacketLimiter enforces a limit on the number of bytes processed over a time window.
 * Implementations should be thread-safe.
 */
public interface PacketLimiter {
  /**
   * Attempts to record the specified number of bytes within the current window.
   *
   * @param bytes the number of bytes to record
   * @return true if the bytes are allowed and recorded; false if the limit would be exceeded
   */
  boolean account(int bytes);
}
