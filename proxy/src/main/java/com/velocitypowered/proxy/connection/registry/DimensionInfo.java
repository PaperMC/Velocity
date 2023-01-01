/*
 * Copyright (C) 2020-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.registry;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents basic information for a Minecraft dimension.
 */
public final class DimensionInfo {

  private final String registryIdentifier;
  private final String levelName;
  private final boolean isFlat;
  private final boolean isDebugType;

  /**
   * Initializes a new {@link DimensionInfo} instance.
   *
   * @param registryIdentifier the identifier for the dimension from the registry
   * @param levelName          the level name as displayed in the F3 menu and logs
   * @param isFlat             if true will set world lighting below surface-level to not display
   *                           fog
   * @param isDebugType        if true constrains the world to the very limited debug-type world
   */
  public DimensionInfo(String registryIdentifier, @Nullable String levelName,
      boolean isFlat, boolean isDebugType) {
    this.registryIdentifier = Preconditions.checkNotNull(
        registryIdentifier, "registryIdentifier cannot be null");
    Preconditions.checkArgument(registryIdentifier.length() > 0,
        "registryIdentifier cannot be empty");
    this.levelName = levelName;
    this.isFlat = isFlat;
    this.isDebugType = isDebugType;
  }

  public boolean isDebugType() {
    return isDebugType;
  }

  public boolean isFlat() {
    return isFlat;
  }

  public @Nullable String getLevelName() {
    return levelName;
  }

  public String getRegistryIdentifier() {
    return registryIdentifier;
  }
}
