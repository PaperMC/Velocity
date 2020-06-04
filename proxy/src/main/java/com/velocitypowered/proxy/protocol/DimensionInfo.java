package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.connection.MinecraftConnection;

import javax.annotation.Nonnull;

public class DimensionInfo {

  private final @Nonnull String dimensionIdentifier;
  private final @Nonnull String dimensionLevelName;
  private final boolean isFlat;
  private final boolean isDebugType;

  /**
   * Initializes a new {@link DimensionInfo} instance.
   * @param dimensionIdentifier the identifier for the dimension from the registry
   * @param dimensionLevelName the level name as displayed in the F3 menu and logs
   * @param isFlat if true will set world lighting below surface-level to not display fog
   * @param isDebugType if true constrains the world to the very limited debug-type world
   */
  public DimensionInfo(@Nonnull String dimensionIdentifier, @Nonnull String dimensionLevelName,
                       boolean isFlat, boolean isDebugType) {
    if (dimensionIdentifier == null || dimensionIdentifier.isEmpty()
            || dimensionIdentifier.isBlank()) {
      throw new IllegalArgumentException("DimensionRegistryName may not be empty or null");
    }
    this.dimensionIdentifier = dimensionIdentifier;
    if (dimensionLevelName == null || dimensionLevelName.isEmpty()
            || dimensionLevelName.isBlank()) {
      throw new IllegalArgumentException("DimensionLevelName may not be empty or null");
    }
    this.dimensionLevelName = dimensionLevelName;
    this.isFlat = isFlat;
    this.isDebugType = isDebugType;
  }

  public boolean isDebugType() {
    return isDebugType;
  }

  public boolean isFlat() {
    return isFlat;
  }

  public @Nonnull String getDimensionLevelName() {
    return dimensionLevelName;
  }

  public @Nonnull String getDimensionIdentifier() {
    return dimensionIdentifier;
  }
}
