/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network.registry;

public interface Platform {
  Platform JAVA = new Platform() {
    @Override
    public String toString() {
      return "Java Edition";
    }
  };
}
