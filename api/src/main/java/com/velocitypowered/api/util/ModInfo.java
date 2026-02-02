/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

/**
 * Represents the information for a Forge mod list.
 */
public final class ModInfo {

  /**
   * The default mod info used when no mods are present.
   * Typically used for Forge-compatible connections that require a placeholder.
   */
  public static final ModInfo DEFAULT = new ModInfo("FML", ImmutableList.of());

  private final String type;
  private final List<Mod> modList;

  /**
   * Constructs a new ModInfo instance.
   *
   * @param type the Forge server list version to use
   * @param modList the mods to present to the client
   */
  public ModInfo(String type, List<Mod> modList) {
    this.type = Preconditions.checkNotNull(type, "type");
    this.modList = ImmutableList.copyOf(modList);
  }

  /**
   * Returns the Forge mod list type (e.g., "FML").
   *
   * @return the mod list type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns an immutable list of all mods in this mod list.
   *
   * @return the list of mods
   */
  public List<Mod> getMods() {
    return modList;
  }

  @Override
  public String toString() {
    return "ModInfo{"
        + "type='" + type + '\''
        + ", modList=" + modList
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModInfo modInfo = (ModInfo) o;
    return type.equals(modInfo.type) && modList.equals(modInfo.modList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, modList);
  }

  /**
   * Represents a mod to send to the client.
   */
  public static final class Mod {

    @SerializedName("modid")
    private final String id;
    private final String version;

    /**
     * Creates a new mod info.
     *
     * @param id the mod identifier
     * @param version the mod version
     */
    public Mod(String id, String version) {
      this.id = Preconditions.checkNotNull(id, "id");
      this.version = Preconditions.checkNotNull(version, "version");
      Preconditions.checkArgument(id.length() < 128, "mod id is too long");
      Preconditions.checkArgument(version.length() < 128, "mod version is too long");
    }

    /**
     * Returns the mod ID (identifier string).
     *
     * @return the mod ID
     */
    public String getId() {
      return id;
    }

    /**
     * Returns the mod version string.
     *
     * @return the mod version
     */
    public String getVersion() {
      return version;
    }

    @Override
    public String toString() {
      return "Mod{"
          + "id='" + id + '\''
          + ", version='" + version + '\''
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Mod mod = (Mod) o;
      return id.equals(mod.id) && version.equals(mod.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, version);
    }
  }
}