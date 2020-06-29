package com.velocitypowered.proxy.util;

import java.time.Duration;

/**
 * Provides utility functions for working with durations.
 */
public class DurationUtils {
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
  public static long convertDurationToTicks(Duration duration) {
    return duration.toMillis() / ONE_TICK_IN_MILLISECONDS;
  }
}
