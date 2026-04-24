/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for generating random UUID v4 values using {@link ThreadLocalRandom}.
 */
public class FastRandomUuid {

  /**
   * Generates a random UUID v4 using {@link ThreadLocalRandom} instead of
   * {@link java.security.SecureRandom}, avoiding contention on the shared secure random instance.
   *
   * @return a new random {@link UUID}
   */
  public static UUID generate() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    long msb = (random.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L; // version 4
    long lsb = (random.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L; // IETF variant
    return new UUID(msb, lsb);
  }
}
