package com.velocitypowered.proxy.connection.registry;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import net.kyori.nbt.CompoundTag;

public final class DimensionData {
  private final String registryIdentifier;
  private final boolean isNatural;
  private final float ambientLight;
  private final boolean isShrunk;
  private final boolean isUltrawarm;
  private final boolean hasCeiling;
  private final boolean hasSkylight;
  private final Optional<Long> fixedTime;
  private final Optional<Boolean> hasEnderdragonFight;

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
  public DimensionData(String registryIdentifier, boolean isNatural,
                       float ambientLight, boolean isShrunk, boolean isUltrawarm,
                       boolean hasCeiling, boolean hasSkylight,
                       Optional<Long> fixedTime, Optional<Boolean> hasEnderdragonFight) {
    Preconditions.checkNotNull(
            registryIdentifier, "registryIdentifier cannot be null");
    Preconditions.checkArgument(registryIdentifier.length() > 0 && !registryIdentifier.isBlank(),
            "registryIdentifier cannot be empty");
    this.registryIdentifier = registryIdentifier;
    this.isNatural = isNatural;
    this.ambientLight = ambientLight;
    this.isShrunk = isShrunk;
    this.isUltrawarm = isUltrawarm;
    this.hasCeiling = hasCeiling;
    this.hasSkylight = hasSkylight;
    this.fixedTime = Preconditions.checkNotNull(
            fixedTime, "fixedTime optional object cannot be null");
    this.hasEnderdragonFight = Preconditions.checkNotNull(
            hasEnderdragonFight, "hasEnderdragonFight optional object cannot be null");
  }

  public String getRegistryIdentifier() {
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

  public Optional<Long> getFixedTime() {
    return fixedTime;
  }

  public Optional<Boolean> getHasEnderdragonFight() {
    return hasEnderdragonFight;
  }

  /**
   * Parses a given CompoundTag to a DimensionData instance.
   * @param toRead the compound from the registry to read
   * @return game dimension data
   */
  public static DimensionData decodeCompoundTag(CompoundTag toRead) {
    Preconditions.checkNotNull(toRead, "CompoundTag cannot be null");
    String registryIdentifier = toRead.getString("key");
    CompoundTag values = toRead.getCompound("element");
    boolean isNatural = values.getBoolean("natural");
    float ambientLight = values.getFloat("ambient_light");
    boolean isShrunk = values.getBoolean("shrunk");
    boolean isUltrawarm = values.getBoolean("ultrawarm");
    boolean hasCeiling = values.getBoolean("has_ceiling");
    boolean hasSkylight = values.getBoolean("has_skylight");
    Optional<Long> fixedTime = Optional.fromNullable(
            values.contains("fixed_time")
                    ? values.getLong("fixed_time") : null);
    Optional<Boolean> hasEnderdragonFight = Optional.fromNullable(
            values.contains("has_enderdragon_fight")
                    ? values.getBoolean("has_enderdragon_fight") : null);
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
    if (fixedTime.isPresent()) {
      values.putLong("fixed_time", fixedTime.get());
    }
    if (hasEnderdragonFight.isPresent()) {
      values.putBoolean("has_enderdragon_fight", hasEnderdragonFight.get());
    }
    ret.put("element", values);
    return ret;
  }
}
