/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.team;

import java.util.Set;
import net.kyori.adventure.text.Component;

/**
 * The interface for managing custom created proxy
 * teams additional to the vanilla teams.
 */
public interface Team {

  /**
   * Method for getting the name and identifier for this team.
   *
   * @return the name and identifier for this team
   */
  String getName();

  /**
   * Method for getting the displayed name for this team.
   *
   * @return the displayed name for this team
   */
  Component getDisplayName();

  /**
   * Method for setting the displayed name for this team.
   *
   * @param displayName the displayed name for this team
   */
  void setDisplayName(Component displayName);

  /**
   * Method for getting the prefix for this team.
   *
   * @return the prefix for this team
   */
  Component getPrefix();

  /**
   * Method for setting the prefix for this team.
   *
   * @param prefix the prefix for this team
   */
  void setPrefix(Component prefix);

  /**
   * Method for getting the suffix for this team.
   *
   * @return the suffix for this team
   */
  Component getSuffix();

  /**
   * Method for setting the suffix for this team.
   *
   * @param suffix the suffix for this team
   */
  void setSuffix(Component suffix);

  /**
   * Method for getting the color for this team.
   * Will only be sent on minecraft version 1.8 and above, the prefix will still be colored.
   *
   * @return the color for this team
   */
  TeamColor getColor();

  /**
   * Method for setting the color for this team.
   * Will only be sent on minecraft version 1.8 and above, the prefix will still be colored.
   *
   * @param color the color for this team
   */
  void setColor(TeamColor color);

  /**
   * Method for getting the friendly fire status for this team.
   *
   * @return the friendly fire status for this team
   */
  boolean isAllowFriendlyFire();

  /**
   * Method for setting the friendly fire status for this team.
   *
   * @param allowFriendlyFire the friendly fire status for this team
   */
  void setAllowFriendlyFire(boolean allowFriendlyFire);

  /**
   * Method for getting the friendly invisibility status for this team.
   *
   * @return the friendly invisibility status for this team
   */
  boolean canSeeFriendlyInvisibles();

  /**
   * Method for setting the friendly invisibility status for this team.
   *
   * @param canSeeFriendlyInvisibles the friendly invisibility status for this team
   */
  void setCanSeeFriendlyInvisibles(boolean canSeeFriendlyInvisibles);

  /**
   * Method for getting the name tag visibility for this team.
   * Will only work on minecraft version 1.8 and above.
   *
   * @return the name tag visibility for this team
   */
  NameTagVisibility getNameTagVisibility();

  /**
   * Method for setting the name tag visibility for this team.
   * Will only work on minecraft version 1.8 and above.
   *
   * @param nameTagVisibility the name tag visibility for this team
   */
  void setNameTagVisibility(NameTagVisibility nameTagVisibility);

  /**
   * Method for getting the collision rule for this team.
   * Will only work on minecraft version 1.9 and above.
   *
   * @return the collision rule for this team
   */
  CollisionRule getCollisionRule();

  /**
   * Method for setting the collision rule for this team.
   * Will only work on minecraft version 1.9 and above.
   *
   * @param collisionRule the collision rule for this team
   */
  void setCollisionRule(CollisionRule collisionRule);

  /**
   * Method for getting the entries for this team.
   *
   * @return the entries for this team
   */
  Set<String> getEntries();

  /**
   * Method for adding one entry to this team.
   *
   * @param entry the entry to be added
   * @return true if it was successful
   */
  boolean addEntry(String entry);

  /**
   * Method for removing one entry from this team.
   *
   * @param entry the entry to be removed
   * @return true if it was successful
   */
  boolean removeEntry(String entry);

  /**
   * Method for checking if this team contains
   * the specified entry.
   *
   * @param entry the entry to be checked
   * @return true if the team contains this entry
   */
  boolean hasEntry(String entry);
}
