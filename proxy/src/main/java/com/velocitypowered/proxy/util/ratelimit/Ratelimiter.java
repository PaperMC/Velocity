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

package com.velocitypowered.proxy.util.ratelimit;

import java.net.InetAddress;

/**
 * Allows rate limiting of clients.
 */
public interface Ratelimiter {

  /**
   * Determines whether or not to allow the connection.
   *
   * @param address the address to rate limit
   * @return true if allowed, false if not
   */
  boolean attempt(InetAddress address);
}
