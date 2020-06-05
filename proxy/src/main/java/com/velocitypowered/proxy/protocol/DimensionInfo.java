package com.velocitypowered.proxy.protocol;

import javax.annotation.Nonnull;

public class DimensionInfo {

  private final @Nonnull String registryIdentifier;
  private final @Nonnull String levelName;
  private final boolean isFlat;
  private final boolean isDebugType;

  /**
   * Initializes a new {@link DimensionInfo} instance.
   * @param registryIdentifier the identifier for the dimension from the registry
   * @param levelName the level name as displayed in the F3 menu and logs
   * @param isFlat if true will set world lighting below surface-level to not display fog
   * @param isDebugType if true constrains the world to the very limited debug-type world
   */
  public DimensionInfo(@Nonnull String registryIdentifier, @Nonnull String levelName,
                       boolean isFlat, boolean isDebugType) {
    if (registryIdentifier == null || registryIdentifier.isEmpty()
            || registryIdentifier.isBlank()) {
      throw new IllegalArgumentException("Dimension registry identifier may not be empty or null");
    }
    this.registryIdentifier = registryIdentifier;
    if (levelName == null || levelName.isEmpty()
            || levelName.isBlank()) {
      throw new IllegalArgumentException("dimensions level name may not be empty or null");
    }
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

  public @Nonnull String getLevelName() {
    return levelName;
  }

  public @Nonnull String getRegistryIdentifier() {
    return registryIdentifier;
  }
}
