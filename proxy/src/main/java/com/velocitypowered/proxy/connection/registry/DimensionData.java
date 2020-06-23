package com.velocitypowered.proxy.connection.registry;

import com.google.common.base.Preconditions;
import net.kyori.nbt.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DimensionData {
  private final String registryIdentifier;
  private final boolean isNatural;
  private final float ambientLight;
  private final boolean isShrunk;
  private final boolean isUltrawarm;
  private final boolean hasCeiling;
  private final boolean hasSkylight;
  private final boolean isPiglinSafe;
  private final boolean doBedsWork;
  private final boolean doRespawnAnchorsWork;
  private final boolean hasRaids;
  private final int logicalHeight;
  private final String burningBehaviourIdentifier;
  private final @Nullable Long fixedTime;
  private final @Nullable Boolean createDragonFight;

  /**
   * Initializes a new {@link DimensionData} instance.
   * @param registryIdentifier the identifier for the dimension from the registry.
   * @param isNatural indicates if the dimension use natural world generation (e.g. overworld)
   * @param ambientLight the light level the client sees without external lighting
   * @param isShrunk indicates if the world is shrunk, aka not the full 256 blocks (e.g. nether)
   * @param isUltrawarm internal dimension warmth flag
   * @param hasCeiling indicates if the dimension has a ceiling layer
   * @param hasSkylight indicates if the dimension should display the sun
   * @param isPiglinSafe indicates if piglins should naturally zombify in this dimension
   * @param doBedsWork indicates if players should be able to sleep in beds in this dimension
   * @param doRespawnAnchorsWork indicates if player respawn points can be used in this dimension
   * @param hasRaids indicates if raids can be spawned in the dimension
   * @param logicalHeight the natural max height for the given dimension
   * @param burningBehaviourIdentifier the identifier for how burning blocks work in the dimension
   * @param fixedTime optional. If set to any game daytime value will deactivate time cycle
   * @param createDragonFight optional. Internal flag used in the end dimension
   */
  public DimensionData(String registryIdentifier, boolean isNatural,
                       float ambientLight, boolean isShrunk, boolean isUltrawarm,
                       boolean hasCeiling, boolean hasSkylight,
                       boolean isPiglinSafe, boolean doBedsWork,
                       boolean doRespawnAnchorsWork, boolean hasRaids,
                       int logicalHeight, String burningBehaviourIdentifier,
                       @Nullable Long fixedTime, @Nullable Boolean createDragonFight) {
    Preconditions.checkNotNull(
            registryIdentifier, "registryIdentifier cannot be null");
    Preconditions.checkArgument(registryIdentifier.length() > 0 && !registryIdentifier.isBlank(),
            "registryIdentifier cannot be empty");
    Preconditions.checkArgument(logicalHeight >= 0, "localHeight must be >= 0");
    Preconditions.checkNotNull(
            burningBehaviourIdentifier, "burningBehaviourIdentifier cannot be null");
    Preconditions.checkArgument(burningBehaviourIdentifier.length() > 0
                    && !burningBehaviourIdentifier.isBlank(),
            "burningBehaviourIdentifier cannot be empty");
    this.registryIdentifier = registryIdentifier;
    this.isNatural = isNatural;
    this.ambientLight = ambientLight;
    this.isShrunk = isShrunk;
    this.isUltrawarm = isUltrawarm;
    this.hasCeiling = hasCeiling;
    this.hasSkylight = hasSkylight;
    this.isPiglinSafe = isPiglinSafe;
    this.doBedsWork = doBedsWork;
    this.doRespawnAnchorsWork = doRespawnAnchorsWork;
    this.hasRaids = hasRaids;
    this.logicalHeight = logicalHeight;
    this.burningBehaviourIdentifier = burningBehaviourIdentifier;
    this.fixedTime = fixedTime;
    this.createDragonFight = createDragonFight;
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

  public boolean hasCeiling() {
    return hasCeiling;
  }

  public boolean hasSkylight() {
    return hasSkylight;
  }

  public boolean isPiglinSafe() {
    return isPiglinSafe;
  }

  public boolean doBedsWork() {
    return doBedsWork;
  }

  public boolean doRespawnAnchorsWork() {
    return doRespawnAnchorsWork;
  }

  public boolean hasRaids() {
    return hasRaids;
  }

  public int getLogicalHeight() {
    return logicalHeight;
  }

  public String getBurningBehaviourIdentifier() {
    return burningBehaviourIdentifier;
  }

  public @Nullable Long getFixedTime() {
    return fixedTime;
  }

  public @Nullable Boolean getCreateDragonFight() {
    return createDragonFight;
  }

  /**
   * Parses a given CompoundTag to a DimensionData instance.
   * @param toRead the compound from the registry to read
   * @return game dimension data
   */
  public static DimensionData decodeCompoundTag(CompoundTag toRead) {
    Preconditions.checkNotNull(toRead, "CompoundTag cannot be null");
    String registryIdentifier = toRead.getString("name");
    boolean isNatural = toRead.getBoolean("natural");
    float ambientLight = toRead.getFloat("ambient_light");
    boolean isShrunk = toRead.getBoolean("shrunk");
    boolean isUltrawarm = toRead.getBoolean("ultrawarm");
    boolean hasCeiling = toRead.getBoolean("has_ceiling");
    boolean hasSkylight = toRead.getBoolean("has_skylight");
    boolean isPiglinSafe = toRead.getBoolean("piglin_safe");
    boolean doBedsWork = toRead.getBoolean("bed_works");
    boolean doRespawnAnchorsWork = toRead.getBoolean("respawn_anchor_works");
    boolean hasRaids = toRead.getBoolean("has_raids");
    int logicalHeight = toRead.getInt("logical_height");
    String burningBehaviourIdentifier = toRead.getString("infiniburn");
    Long fixedTime = toRead.contains("fixed_time")
            ? toRead.getLong("fixed_time") : null;
    Boolean hasEnderdragonFight = toRead.contains("has_enderdragon_fight")
                    ? toRead.getBoolean("has_enderdragon_fight") : null;
    return new DimensionData(
            registryIdentifier, isNatural, ambientLight, isShrunk,
            isUltrawarm, hasCeiling, hasSkylight, isPiglinSafe, doBedsWork, doRespawnAnchorsWork,
            hasRaids, logicalHeight, burningBehaviourIdentifier, fixedTime, hasEnderdragonFight);
  }

  /**
   * Encodes the Dimension data as CompoundTag.
   * @return compound containing the dimension data
   */
  public CompoundTag encodeAsCompundTag() {
    CompoundTag ret = new CompoundTag();
    ret.putString("name", registryIdentifier);
    ret.putBoolean("natural", isNatural);
    ret.putFloat("ambient_light", ambientLight);
    ret.putBoolean("shrunk", isShrunk);
    ret.putBoolean("ultrawarm", isUltrawarm);
    ret.putBoolean("has_ceiling", hasCeiling);
    ret.putBoolean("has_skylight", hasSkylight);
    ret.putBoolean("piglin_safe", isPiglinSafe);
    ret.putBoolean("bed_works", doBedsWork);
    ret.putBoolean("respawn_anchor_works", doRespawnAnchorsWork);
    ret.putBoolean("has_raids", hasRaids);
    ret.putInt("logical_height", logicalHeight);
    ret.putString("infiniburn", burningBehaviourIdentifier);
    if (fixedTime != null) {
      ret.putLong("fixed_time", fixedTime);
    }
    if (createDragonFight != null) {
      ret.putBoolean("has_enderdragon_fight", createDragonFight);
    }
    return ret;
  }
}
