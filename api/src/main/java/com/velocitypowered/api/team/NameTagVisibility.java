/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.team;

/**
 * All possible name tag visibilities which can be applied to a {@link Team}
 * using {@link Team#setNameTagVisibility(NameTagVisibility)} and queried using
 * {@link Team#getNameTagVisibility()}.
 */
public enum NameTagVisibility {

  ALWAYS("always"),
  HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
  HIDE_FOR_OWN_TEAM("hideForOwnTeam"),
  NEVER("never");

  private final String action;

  NameTagVisibility(String action) {
    this.action = action;
  }

  public String getAction() {
    return action;
  }
}
