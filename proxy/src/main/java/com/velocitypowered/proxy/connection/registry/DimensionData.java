package com.velocitypowered.proxy.connection.registry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.kyori.nbt.CompoundTag;

public class DimensionData {
  private final @Nonnull String registryIdentifier;
  private final boolean isNatural;
  private final float ambientLight;
  private final boolean isShrunk;
  private final boolean isUltrawarm;
  private final boolean hasCeiling;
  private final boolean hasSkylight;
  private final @Nullable Long fixedTime;
  private final @Nullable Boolean hasEnderdragonFight;

  /**
   * Initializes a new {@link DimensionData} instance.
   * @param registryIdentifier the identifier for the dimension from the registry.
   * @param isNatural indicates if the dimension use natural world generation (e.g. overworld)
   * @param ambientLight the light level the client sees without external lighting
   * @param isShrunk indicates if the world is shrunk, aka not the full 256 blocks (e.g. nether)
   * @param isUltrawarm internal dimension warmth flag
   * @param hasCeiling indicates if the dimension has a ceiling layer
   * @param hasSkylight indicates if the dimension should display the sun
   * @param fixedTime optional. If set to any game daytime value will deactivate time cycle
   * @param hasEnderdragonFight optional. Internal flag used in the end dimension
   */
  public DimensionData(@Nonnull String registryIdentifier, boolean isNatural,
                       float ambientLight, boolean isShrunk, boolean isUltrawarm,
                       boolean hasCeiling, boolean hasSkylight,
                       @Nullable Long fixedTime, @Nullable Boolean hasEnderdragonFight) {
    this.registryIdentifier = registryIdentifier;
    this.isNatural = isNatural;
    this.ambientLight = ambientLight;
    this.isShrunk = isShrunk;
    this.isUltrawarm = isUltrawarm;
    this.hasCeiling = hasCeiling;
    this.hasSkylight = hasSkylight;
    this.fixedTime = fixedTime;
    this.hasEnderdragonFight = hasEnderdragonFight;
  }

  public @Nonnull String getRegistryIdentifier() {
    return registryIdentifier;
  }

  public boolean isNatural() {
    return isNatural;
  }

  public float getAmbientLight() {
    return ambientLight;
  }

  public boolean isShrunk() {
    return isShrunk;
  }

  public boolean isUltrawarm() {
    return isUltrawarm;
  }

  public boolean isHasCeiling() {
    return hasCeiling;
  }

  public boolean isHasSkylight() {
    return hasSkylight;
  }

  public @Nullable Long getFixedTime() {
    return fixedTime;
  }

  public @Nullable Boolean getHasEnderdragonFight() {
    return hasEnderdragonFight;
  }

  /**
   * Parses a given CompoundTag to a DimensionData instance.
   * @param toRead the compound from the registry to read
   * @return game dimension data
   */
  public static DimensionData fromCompoundTag(@Nonnull CompoundTag toRead) {
    if (toRead == null) {
      throw new IllegalArgumentException("CompoundTag cannot be null");
    }
    String registryIdentifier = toRead.getString("key");
    CompoundTag values = toRead.getCompound("element");
    boolean isNatural = values.getBoolean("natural");
    float ambientLight = values.getFloat("ambient_light");
    boolean isShrunk = values.getBoolean("shrunk");
    boolean isUltrawarm = values.getBoolean("ultrawarm");
    boolean hasCeiling = values.getBoolean("has_ceiling");
    boolean hasSkylight = values.getBoolean("has_skylight");
    Long fixedTime = values.contains("fixed_time") ? values.getLong("fixed_time") : null;
    Boolean hasEnderdragonFight = values.contains("has_enderdragon_fight")
            ? values.getBoolean("has_enderdragon_fight") : null;
    return new DimensionData(
            registryIdentifier, isNatural, ambientLight, isShrunk,
            isUltrawarm, hasCeiling, hasSkylight, fixedTime, hasEnderdragonFight);
  }

  /**
   * Encodes the Dimension data as CompoundTag.
   * @return compound containing the dimension data
   */
  public CompoundTag encodeAsCompundTag() {
    CompoundTag ret = new CompoundTag();
    ret.putString("key", registryIdentifier);
    CompoundTag values = new CompoundTag();
    values.putBoolean("natural", isNatural);
    values.putFloat("ambient_light", ambientLight);
    values.putBoolean("shrunk", isShrunk);
    values.putBoolean("ultrawarm", isUltrawarm);
    values.putBoolean("has_ceiling", hasCeiling);
    values.putBoolean("has_skylight", hasSkylight);
    if (fixedTime != null) {
      values.putLong("fixed_time", fixedTime);
    }
    if (hasEnderdragonFight != null) {
      values.putBoolean("has_enderdragon_fight", hasEnderdragonFight);
    }
    ret.put("element", values);
    return ret;
  }
}
