/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.team;

/**
 * All possible collision rules which can be applied to a {@link Team}
 * using {@link Team#setCollisionRule(CollisionRule)} and queried using
 * {@link Team#getCollisionRule()}.
 */
public enum CollisionRule {

  ALWAYS("always"),
  PUSH_OTHER_TEAMS("pushOtherTeams"),
  PUSH_OWN_TEAM("pushOwnTeam"),
  NEVER("never");

  private final String action;

  CollisionRule(String action) {
    this.action = action;
  }

  public String getAction() {
    return action;
  }
}
