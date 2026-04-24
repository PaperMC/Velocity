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
 * A utility class for generating random UUIDs without the overhead of secure random generation.
 * Not official UUID v4 version, but should be sufficient for most use cases.
 */
public class FastRandomUuid {
  public static UUID generate() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return new UUID(random.nextLong(), random.nextLong());
  }
}
