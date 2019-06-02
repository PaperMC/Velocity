package com.velocitypowered.api.bossbar;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a color of a {@link BossBar}
 */
public enum BarColor {
  PINK(0),
  BLUE(1),
  RED(2),
  GREEN(3),
  YELLOW(4),
  PURPLE(5),
  WHITE(6);

  private static final Map<Integer, BarColor> byInteger = new ConcurrentHashMap<>();

  static {
    for(BarColor color : BarColor.values()) {
      byInteger.put(color.getIntValue(), color);
    }
  }

  private final int intValue;

  BarColor(int intValue) {
    this.intValue = intValue;
  }

  /**
   * Gets a {@link BossBar} color by {@link Integer}
   *
   * @param value color integer
   * @return representing color, or <code>null</code> if not found
   */
  @Nullable
  public static BarColor getByInteger(int value) {
    return byInteger.get(value);
  }

  /**
   * Gets the color's {@link Integer} value
   *
   * @return int value of the color
   */
  public int getIntValue() {
    return intValue;
  }
}
