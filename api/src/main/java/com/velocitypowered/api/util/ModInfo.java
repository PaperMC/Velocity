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

  public String getType() {
    return type;
  }

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

    public Mod(String id, String version) {
      this.id = Preconditions.checkNotNull(id, "id");
      this.version = Preconditions.checkNotNull(version, "version");
    }

    public String getId() {
      return id;
    }

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