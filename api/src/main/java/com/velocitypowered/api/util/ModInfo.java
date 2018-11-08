package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class ModInfo {

  public static final ModInfo DEFAULT = new ModInfo("FML", ImmutableList.of());

  private final String type;
  private final List<Mod> modList;

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

  public static final class Mod {

    private final String modid;
    private final String version;

    public Mod(String modid, String version) {
      this.modid = Preconditions.checkNotNull(modid, "modid");
      this.version = Preconditions.checkNotNull(version, "version");
    }

    public String getId() {
      return modid;
    }

    public String getVersion() {
      return version;
    }

    @Override
    public String toString() {
      return "Mod{"
          + "modid='" + modid + '\''
          + ", version='" + version + '\''
          + '}';
    }
  }
}