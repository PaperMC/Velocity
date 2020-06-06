package com.velocitypowered.proxy.connection.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.ListTag;
import net.kyori.nbt.Tag;
import net.kyori.nbt.TagType;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class DimensionRegistry {

  private final Map<String, DimensionData> registeredDimensions;
  private final ImmutableSet<String> levelNames;

  /**
   * Initializes a new {@link DimensionRegistry} instance.
   * This registry is required for 1.16+ clients/servers to communicate,
   * it constrains the dimension types and names the client can be sent
   * in a Respawn action (dimension change).
   * This WILL raise an IllegalArgumentException if the following is not met:
   * - At least one valid DimensionData instance is provided
   * - At least one valid world name is provided
   * @param registeredDimensions a populated {@link ImmutableSet} containing dimension data types
   * @param levelNames a populated {@link ImmutableSet} of the level (world) names the server offers
   */
  public DimensionRegistry(ImmutableSet<DimensionData> registeredDimensions,
                            ImmutableSet<String> levelNames) {
    Preconditions.checkNotNull(registeredDimensions,
            "registeredDimensions cannot be null");
    Preconditions.checkNotNull(levelNames,
            "levelNames cannot be null");
    Preconditions.checkArgument(registeredDimensions.size() > 0,
            "registeredDimensions needs to be populated");
    Preconditions.checkArgument(levelNames.size() > 0,
            "levelNames needs to populated");
    this.registeredDimensions = Maps.uniqueIndex(
            registeredDimensions, DimensionData::getRegistryIdentifier);
    this.levelNames = levelNames;
  }

  public Map<String, DimensionData> getRegisteredDimensions() {
    return registeredDimensions;
  }

  public Set<String> getLevelNames() {
    return levelNames;
  }

  /**
   * Returns the internal dimension data type as used by the game.
   * @param dimensionIdentifier how the dimension is identified by the connection
   * @return game dimension data or null if not registered
   */
  public @Nullable DimensionData getDimensionData(String dimensionIdentifier) {
    return registeredDimensions.getOrDefault(dimensionIdentifier, null);
  }

  /**
   * Checks a {@link DimensionInfo} against this registry.
   * @param toValidate the {@link DimensionInfo} to validate
   * @return true: the dimension information is valid for this registry
   */
  public boolean isValidFor(DimensionInfo toValidate) {
    if (toValidate == null) {
      return false;
    }
    return registeredDimensions.containsKey(toValidate.getRegistryIdentifier())
            && levelNames.contains(toValidate.getLevelName());
  }

  /**
   * Encodes the stored Dimension registry as CompoundTag.
   * @return the CompoundTag containing identifier:type mappings
   */
  public CompoundTag encodeRegistry() {
    CompoundTag ret = new CompoundTag();
    ListTag list = new ListTag(TagType.COMPOUND);
    for (DimensionData iter : registeredDimensions.values()) {
      list.add(iter.encodeAsCompundTag());
    }
    ret.put("dimension", list);
    return ret;
  }

  /**
   * Decodes a CompoundTag storing a dimension registry.
   * @param toParse CompoundTag containing a dimension registry
   */
  public static ImmutableSet<DimensionData> fromGameData(CompoundTag toParse) {
    Preconditions.checkNotNull(toParse, "CompoundTag cannot be null");
    Preconditions.checkArgument(toParse.contains("dimension", TagType.LIST),
            "CompoundTag does not contain a dimension list");
    ListTag dimensions = toParse.getList("dimension");
    ImmutableSet.Builder<DimensionData> mappings = ImmutableSet.builder();
    for (Tag iter : dimensions) {
      if (iter instanceof CompoundTag) {
        mappings.add(DimensionData.decodeCompoundTag((CompoundTag) iter));
      }
    }
    return mappings.build();
  }
}
