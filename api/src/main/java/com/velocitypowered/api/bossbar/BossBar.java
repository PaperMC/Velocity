package com.velocitypowered.api.bossbar;

import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import java.util.UUID;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a boss bar, which can be send to a (group of) player(s) <b>WARNING!!!</b> This works
 * only on 1.9+. <b>YOU ARE ON YOUR OWN IF YOU USE THIS ON 1.8</b>
 */
public interface BossBar {

  /**
   * Adds all specified players to this boss bar
   *
   * @param players players
   * @see #addPlayer(Player)
   */
  void addPlayers(@NonNull Iterable<Player> players);

  /**
   * Adds player to this boss bar. This adds the player to the {@link #getPlayers()} and makes him
   * see the boss bar
   *
   * @param player the player you wish to add
   */
  void addPlayer(@NonNull Player player);

  /**
   * Removes player from this boss bar. This removes the player from {@link #getPlayers()} and makes
   * him not see the boss bar
   *
   * @param player the player you wish to remove
   */
  void removePlayer(@NonNull Player player);

  /**
   * Removes all specified players from this boss bar
   *
   * @param players players
   * @see #removePlayer(Player)
   */
  void removePlayers(@NonNull Iterable<Player> players);

  /**
   * Removes all players, that see this boss bar.
   *
   * @see #removePlayer(Player)
   */
  void removeAllPlayers();

  /**
   * Gets the title of this boss bar.
   *
   * @return title
   */
  @NonNull
  Component getTitle();

  /**
   * Sets a new title of the boss bar.
   *
   * @param title new title
   */
  void setTitle(@NonNull Component title);

  /**
   * Gets the boss bar's progress. In Minecraft, this is called 'health' of the boss bar
   *
   * @return progress
   */
  float getProgress();

  /**
   * Sets a new progress of the boss bar. In Minecraft, this is called 'health' of the boss bar.
   *
   * @param progress a float between 0 and 1, representing boss bar's progress
   * @throws IllegalArgumentException if the new progress is not between 0 and 1
   */
  void setProgress(float progress);

  /**
   * Returns a unmodifiable {@link Collection} of all {@link Player} added to the boss bar.
   *
   * @return players
   */
  @Nullable
  Collection<Player> getPlayers();

  /**
   * Gets the {@link UUID} of the boss bar.
   *
   * @return uuid
   */
  @NonNull
  UUID getUUID();

  /**
   * Gets the color of the boss bar.
   *
   * @return boss bar color
   */
  @NonNull
  BossBarColor getColor();

  /**
   * Sets a new color of the boss bar
   *
   * @param color the color you wish the boss bar be displayed with
   */
  void setColor(@NonNull BossBarColor color);

  /**
   * Gets the overlay of the boss bar.
   *
   * @return boss bar overlay
   */
  @NonNull
  BossBarOverlay getOverlay();

  /**
   * Sets a new overlay of the boss bar
   *
   * @param overlay the overlay you wish the boss bar be displayed with
   */
  void setOverlay(@NonNull BossBarOverlay overlay);

  /**
   * Returns whenever this boss bar is visible to all added {@link #getPlayers()}. By default, it
   * returns <code>true</code>
   *
   * @return <code>true</code> if visible, otherwise <code>false</code>
   */
  boolean isVisible();

  /**
   * Sets a new visibility to the boss bar
   *
   * @param visible boss bar visibility value
   */
  void setVisible(boolean visible);
}
