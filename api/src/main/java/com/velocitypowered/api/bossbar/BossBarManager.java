package com.velocitypowered.api.bossbar;

import java.util.Collection;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a boss bar manager
 */
public interface BossBarManager {

  /**
   * Creates a new {@link BossBar}
   *
   * @param title boss bar title
   * @param color boss bar color
   * @param overlay boss bar overlay
   * @return a fresh boss bar
   */
  @NonNull
  BossBar create(@NonNull Component title, @NonNull BossBarColor color,
      @NonNull BossBarOverlay overlay);

  /**
   * Creates a new {@link BossBar}
   *
   * @param title boss bar title
   * @param color boss bar color
   * @param overlay boss bar overlay
   * @param progress boss bar progress
   * @return a fresh boss bar
   * @throws IllegalArgumentException if progress not between 0 and 1
   */
  @NonNull
  BossBar create(
      @NonNull Component title, @NonNull BossBarColor color, @NonNull BossBarOverlay overlay,
      float progress);

  /**
   * Returns a copy of all registered {@link BossBar}'s.
   *
   * @return all boss bars
   */
  Collection<BossBar> getRegisteredBossBars();
}
