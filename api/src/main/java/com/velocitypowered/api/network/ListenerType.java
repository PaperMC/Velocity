/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network;

/**
 * Represents each listener type.
 */
public enum ListenerType {
  MINECRAFT("Minecraft"),
  QUERY("Query");

  final String name;

  ListenerType(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
