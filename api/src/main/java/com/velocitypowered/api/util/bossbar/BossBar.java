/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.bossbar;

import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import net.kyori.text.Component;

/**
 * Represents a boss bar, which can be send to a (group of) player(s).
 * <b>Boss bars only work on 1.9 and above.</b>
 *
 * @deprecated Replaced with {@link net.kyori.adventure.bossbar.BossBar}
 */
@Deprecated
public interface BossBar {

  /**
   * Adds all specified players to this boss bar.
   *
   * @param players players
   * @see #addPlayer(Player)
   */
  void addPlayers(Iterable<Player> players);

  /**
   * Adds player to this boss bar. This adds the player to the {@link #getPlayers()} and makes him
   * see the boss bar.
   *
   * @param player the player you wish to add
   */
  void addPlayer(Player player);

  /**
   * Removes player from this boss bar. This removes the player from {@link #getPlayers()} and makes
   * him not see the boss bar.
   *
   * @param player the player you wish to remove
   */
  void removePlayer(Player player);

  /**
   * Removes all specified players from this boss bar.
   *
   * @param players players
   * @see #removePlayer(Player)
   */
  void removePlayers(Iterable<Player> players);

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
  Component getTitle();

  /**
   * Sets a new title of the boss bar.
   *
   * @param title new title
   */
  void setTitle(Component title);

  /**
   * Gets the boss bar's percent.
   *
   * @return percent
   */
  float getPercent();

  /**
   * Sets a new percent of the boss bar.
   *
   * @param percent a float between 0 and 1, representing boss bar's percent
   * @throws IllegalArgumentException if the new percent is not between 0 and 1
   */
  void setPercent(float percent);

  /**
   * Returns a copy of the {@link Collection} of all {@link Player} added to the boss bar.
   * <i>Can be empty.</i>
   *
   * @return players
   */
  Collection<Player> getPlayers();

  /**
   * Gets the color of the boss bar.
   *
   * @return boss bar color
   */
  BossBarColor getColor();

  /**
   * Sets a new color of the boss bar.
   *
   * @param color the color you wish the boss bar be displayed with
   */
  void setColor(BossBarColor color);

  /**
   * Gets the overlay of the boss bar.
   *
   * @return boss bar overlay
   */
  BossBarOverlay getOverlay();

  /**
   * Sets a new overlay of the boss bar.
   *
   * @param overlay the overlay you wish the boss bar be displayed with
   */
  void setOverlay(BossBarOverlay overlay);

  /**
   * Returns whenever this boss bar is visible to all added {@link #getPlayers()}. By default, it
   * returns <code>true</code>.
   *
   * @return <code>true</code> if visible, otherwise <code>false</code>
   */
  boolean isVisible();

  /**
   * Sets a new visibility to the boss bar.
   *
   * @param visible boss bar visibility value
   */
  void setVisible(boolean visible);

  /**
   * Returns a copy of of the {@link Collection} of all {@link BossBarFlag}s added to the boss bar.
   *
   * @return flags
   */
  Collection<BossBarFlag> getFlags();

  /**
   * Adds new flags to the boss bar.
   *
   * @param flags the flags you wish to add
   */
  void addFlags(BossBarFlag... flags);

  /**
   * Removes flag from the boss bar.
   *
   * @param flag the flag you wish to remove
   */
  void removeFlag(BossBarFlag flag);

  /**
   * Removes flags from the boss bar.
   *
   * @param flags the flags you wish to remove
   */
  void removeFlags(BossBarFlag... flags);
}
