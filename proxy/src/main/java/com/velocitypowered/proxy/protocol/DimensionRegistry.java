package com.velocitypowered.proxy.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nonnull;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.ListTag;
import net.kyori.nbt.Tag;
import net.kyori.nbt.TagType;

public class DimensionRegistry {

  // Mapping:
  // dimensionIdentifier (Client connection refers to this),
  // dimensionType (The game refers to this).
  private final @Nonnull Map<String, String> dimensionRegistry;
  private final @Nonnull Set<String> worldNames;

  /**
   * Initializes a new {@link DimensionRegistry} instance.
   * This registry is required for 1.16+ clients/servers to communicate,
   * it constrains the dimension types and names the client can be sent
   * in a Respawn action (dimension change).
   * @param dimensionRegistry a populated map containing dimensionIdentifier and dimensionType sets
   * @param worldNames a populated {@link Set} of the dimension level names the server offers
   */
  public DimensionRegistry(Map<String, String> dimensionRegistry,
                           Set<String> worldNames) {
    if (dimensionRegistry == null || dimensionRegistry.isEmpty()
            || worldNames == null || worldNames.isEmpty()) {
      throw new IllegalArgumentException(
              "Dimension registry requires valid arguments, not null and not empty");
    }
    this.dimensionRegistry = dimensionRegistry;
    this.worldNames = worldNames;
  }

  public @Nonnull Map<String, String> getDimensionRegistry() {
    return dimensionRegistry;
  }

  public @Nonnull Set<String> getWorldNames() {
    return worldNames;
  }

  /**
   * Returns the internal dimension type as used by the game.
   * @param dimensionIdentifier how the type is identified by the connection
   * @return game internal dimension type
   */
  public @Nonnull String getDimensionType(@Nonnull String dimensionIdentifier) {
    if (dimensionIdentifier == null) {
      throw new IllegalArgumentException("Dimension identifier cannot be null!");
    }
    if (dimensionIdentifier == null || !dimensionRegistry.containsKey(dimensionIdentifier)) {
      throw new NoSuchElementException("Dimension with identifier " + dimensionIdentifier
              + " doesn't exist in this Registry!");
    }
    return dimensionRegistry.get(dimensionIdentifier);
  }

  /**
   * Returns the dimension identifier as used by the client.
   * @param dimensionType the internal dimension type
   * @return game dimension identifier
   */
  public @Nonnull String getDimensionIdentifier(@Nonnull String dimensionType) {
    if (dimensionType == null) {
      throw new IllegalArgumentException("Dimension type cannot be null!");
    }
    for (Map.Entry<String, String> entry : dimensionRegistry.entrySet()) {
      if (entry.getValue().equals(dimensionType)) {
        return entry.getKey();
      }
    }
    throw new NoSuchElementException("Dimension type " + dimensionType
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
      if (!worldNames.contains(toValidate.getDimensionLevelName())) {
        return false;
      }
      getDimensionType(toValidate.getDimensionIdentifier());
      return true;
    } catch (NoSuchElementException thrown) {
      return false;
    }
  }

  /**
   * Encodes the stored Dimension registry as CompoundTag.
   * @return the CompoundTag containing identifier:type mappings
   */
  public CompoundTag encodeToCompoundTag() {
    CompoundTag ret = new CompoundTag();
    ListTag list = new ListTag(TagType.COMPOUND);
    for (Map.Entry<String, String> entry : dimensionRegistry.entrySet()) {
      CompoundTag item = new CompoundTag();
      item.putString("key", entry.getKey());
      item.putString("element", entry.getValue());
      list.add(item);
    }
    ret.put("dimension", list);
    return ret;
  }

  /**
   * Decodes a CompoundTag storing dimension mappings to a Map identifier:type.
   * @param toParse CompoundTag containing a dimension registry
   */
  public static Map<String, String> parseToMapping(@Nonnull CompoundTag toParse) {
    if (toParse == null) {
      throw new IllegalArgumentException("CompoundTag cannot be null");
    }
    if (!toParse.contains("dimension", TagType.LIST)) {
      throw new IllegalStateException("CompoundTag does not contain a dimension List");
    }
    ListTag dimensions = toParse.getList("dimension");
    Map<String, String> mappings = new HashMap<String, String>();
    for (Tag iter : dimensions) {
      if (iter instanceof CompoundTag) {
        throw new IllegalStateException("DimensionList in CompoundTag contains an invalid entry");
      }
      CompoundTag mapping = (CompoundTag) iter;
      String key = mapping.getString("key", null);
      String element = mapping.getString("element", null);
      if (element == null || key == null) {
        throw new IllegalStateException("DimensionList in CompoundTag contains an mapping");
      }
      if (mappings.containsKey(key) || mappings.containsValue(element)) {
        throw new IllegalStateException(
                "Dimension mappings may not have identifier/name duplicates");
      }
      mappings.put(key, element);
    }
    if (mappings.isEmpty()) {
      throw new IllegalStateException("Dimension mapping cannot be empty");
    }
    return mappings;
  }
}
