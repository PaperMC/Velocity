package com.velocitypowered.proxy.protocol;

import java.util.*;
import javax.annotation.Nonnull;

import com.google.inject.internal.asm.$TypePath;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.ListTag;
import net.kyori.nbt.Tag;
import net.kyori.nbt.TagType;

public class DimensionRegistry {

  private final @Nonnull Set<DimensionData> dimensionRegistry;
  private final @Nonnull String[] levelNames;

  /**
   * Initializes a new {@link DimensionRegistry} instance.
   * This registry is required for 1.16+ clients/servers to communicate,
   * it constrains the dimension types and names the client can be sent
   * in a Respawn action (dimension change).
   * @param dimensionRegistry a populated set containing dimension data types
   * @param levelNames a populated {@link Set} of the dimension level names the server offers
   */
  public DimensionRegistry(Set<DimensionData> dimensionRegistry,
                           String[] levelNames) {
    if (dimensionRegistry == null || dimensionRegistry.isEmpty()
            || levelNames == null || levelNames.length == 0) {
      throw new IllegalArgumentException(
              "Dimension registry requires valid arguments, not null and not empty");
    }
    this.dimensionRegistry = dimensionRegistry;
    this.levelNames = levelNames;
  }

  public @Nonnull Set<DimensionData> getDimensionRegistry() {
    return dimensionRegistry;
  }

  public @Nonnull String[] getLevelNames() {
    return levelNames;
  }

  /**
   * Returns the internal dimension data type as used by the game.
   * @param dimensionIdentifier how the dimension is identified by the connection
   * @return game dimension data
   */
  public @Nonnull DimensionData getDimensionData(@Nonnull String dimensionIdentifier) {
    if (dimensionIdentifier == null) {
      throw new IllegalArgumentException("Dimension identifier cannot be null!");
    }
    for (DimensionData iter : dimensionRegistry) {
      if(iter.getRegistryIdentifier().equals(dimensionIdentifier)) {
        return iter;
      }
    }
    throw new NoSuchElementException("Dimension with identifier " + dimensionIdentifier
            + " doesn't exist in this Registry!");
  }

  /**
   * Checks a {@link DimensionInfo} against this registry.
   * @param toValidate the {@link DimensionInfo} to validate
   * @return true: the dimension information is valid for this registry
   */
  public boolean isValidFor(@Nonnull DimensionInfo toValidate) {
    if (toValidate == null) {
      throw new IllegalArgumentException("Dimension info cannot be null");
    }
    try {
      getDimensionData(toValidate.getDimensionIdentifier());
      for(int i = 0; i < levelNames.length; i++) {
        if(levelNames[i].equals(toValidate.getDimensionIdentifier())) {
          return true;
        }
      }
      return false;
    } catch (NoSuchElementException thrown) {
      return false;
    }
  }

  /**
   * Encodes the stored Dimension registry as CompoundTag.
   * @return the CompoundTag containing identifier:type mappings
   */
  public CompoundTag encodeRegistry() {
    CompoundTag ret = new CompoundTag();
    ListTag list = new ListTag(TagType.COMPOUND);
    for (DimensionData iter : dimensionRegistry) {
      list.add(iter.encode());
    }
    ret.put("dimension", list);
    return ret;
  }

  /**
   * Decodes a CompoundTag storing a dimension registry
   * @param toParse CompoundTag containing a dimension registry
   * @param levelNames world level names
   */
  public static DimensionRegistry fromGameData(@Nonnull CompoundTag toParse, @Nonnull String[] levelNames) {
    if (toParse == null) {
      throw new IllegalArgumentException("CompoundTag cannot be null");
    }
    if (levelNames == null || levelNames.length == 0) {
      throw new IllegalArgumentException("Level names cannot be null or empty");
    }
    if (!toParse.contains("dimension", TagType.LIST)) {
      throw new IllegalStateException("CompoundTag does not contain a dimension List");
    }
    ListTag dimensions = toParse.getList("dimension");
    Set<DimensionData> mappings = new HashSet<DimensionData>();
    for (Tag iter : dimensions) {
      if (!(iter instanceof CompoundTag)) {
        throw new IllegalStateException("DimensionList in CompoundTag contains an invalid entry");
      }
      mappings.add(DimensionData.fromNBT((CompoundTag) iter));
    }
    if (mappings.isEmpty()) {
      throw new IllegalStateException("Dimension mapping cannot be empty");
    }
    return new DimensionRegistry(mappings, levelNames);
  }
}
