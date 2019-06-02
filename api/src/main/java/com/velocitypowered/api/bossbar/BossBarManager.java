package com.velocitypowered.api.bossbar;

import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents a boss bar manager
 */
public interface BossBarManager {

  /**
   * Creates a new {@link BossBar}
   *
   * @param title boss bar title
   * @param color boss bar color
   * @param style boss bar style
   * @return a fresh boss bar
   */
  @NonNull
  BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style);

  /**
   * Creates a new {@link BossBar}
   *
   * @param title boss bar title
   * @param color boss bar color
   * @param style boss bar style
   * @param progress boss bar progress
   * @return a fresh boss bar
   * @throws IllegalArgumentException if progress not between 0 and 1
   */
  @NonNull
  BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style, float progress);

  /**
   * Creates a new {@link BossBar}
   *
   * @param title boss bar title
   * @param color boss bar color
   * @param style boss bar style
   * @param progress boss bar progress
   * @param uuid uuid of the boss bar
   * @return a fresh boss bar, <b>or null if there's already a boss bar with this uuid</b>
   * @throws IllegalArgumentException if progress not between 0 and 1
   */
  @Nullable
  BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style, float progress, @NonNull UUID uuid);

  /**
   * Returns a {@link BossBar} if a one with the specified
   * {@link UUID} is registered, otherwise <code>null</code>
   *
   * @param uuid boss bar unique id
   * @return boss bar or <code>null</code>
   */
  @Nullable
  BossBar get(@NonNull UUID uuid);

  /**
   * "Removes" a boss bar, by removing all players and removing
   * from {@link #getRegisteredBossBars()}
   *
   * @param uuid boss bar uuid
   * @return <code>true</code> if success and removed, otherwise <code>false</code>
   */
  boolean remove(@NonNull UUID uuid);

  /**
   * "Removes" a boss bar, by removing all players and removing
   * from {@link #getRegisteredBossBars()}
   *
   * @param bossBar boss bar
   * @return <code>true</code> if success and removed, otherwise <code>false</code>
   */
  boolean remove(@NonNull BossBar bossBar);

  /**
   * Gets all registered {@link BossBar}'s
   *
   * @return all boss bars
   */
  @Nullable
  Collection<BossBar> getRegisteredBossBars();
}
