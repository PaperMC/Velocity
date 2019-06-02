package com.velocitypowered.api.bossbar;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a style of a {@link BossBar}
 */
public enum BarStyle {
  SOLID(0),
  SEGMENTED_6(1),
  SEGMENTED_10(2),
  SEGMENTED_12(3),
  SEGMENTED_20(4);

  private static final Map<Integer, BarStyle> byInteger = new ConcurrentHashMap<>();

  static {
    for(BarStyle style : BarStyle.values()) {
      byInteger.put(style.getIntValue(), style);
    }
  }

  private final int intValue;

  BarStyle(int intValue) {
    this.intValue = intValue;
  }

  /**
   * Gets a {@link BossBar} style by {@link Integer}
   *
   * @param value style integer
   * @return representing style, or <code>null</code> if not found
   */
  @Nullable
  public static BarStyle getByInteger(int value) {
    return byInteger.get(value);
  }

  /**
   * Gets the style's {@link Integer} value
   *
   * @return int value of the style
   */
  public int getIntValue() {
    return intValue;
  }
}
