/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

import java.time.Duration;

/**
 * Provides utility functions for working with durations.
 */
public final class DurationUtils {

  private static final long ONE_TICK_IN_MILLISECONDS = 50;

  private DurationUtils() {
    throw new AssertionError("Instances of this class should not be created.");
  }

  /**
   * Converts the given duration to Minecraft ticks.
   *
   * @param duration the duration to convert into Minecraft ticks
   * @return the duration represented as the number of Minecraft ticks
   */
  public static long toTicks(Duration duration) {
    return duration.toMillis() / ONE_TICK_IN_MILLISECONDS;
  }
}
