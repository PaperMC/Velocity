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

  private final @Nonnull Map<String, String> dimensionRegistry;
  private final @Nonnull Set<String> worldNames;

  public DimensionRegistry(Map<String, String> dimensionRegistry,
                           Set<String> worldNames) {
    if (dimensionRegistry == null || dimensionRegistry.isEmpty()
            || worldNames == null || worldNames.isEmpty()) {
      throw new IllegalArgumentException(
              "DimensionRegistry requires valid arguments, not null and not empty");
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

  public @Nonnull String getDimensionIdentifier(@Nonnull String dimensionName) {
    if (dimensionName == null) {
      throw new IllegalArgumentException("DimensionName cannot be null!");
    }
    if (dimensionName == null || !dimensionRegistry.containsKey(dimensionName)) {
      throw new NoSuchElementException("DimensionName " + dimensionName
              + " doesn't exist in this Registry!");
    }
    return dimensionRegistry.get(dimensionName);
  }

  public @Nonnull String getDimensionName(@Nonnull String dimensionIdentifier) {
    if (dimensionIdentifier == null) {
      throw new IllegalArgumentException("DimensionIdentifier cannot be null!");
    }
    for (Map.Entry<String, String> entry : dimensionRegistry.entrySet()) {
      if (entry.getValue().equals(dimensionIdentifier)) {
        return entry.getKey();
      }
    }
    throw new NoSuchElementException("DimensionIdentifier " + dimensionIdentifier
            + " doesn't exist in this Registry!");
  }

  public boolean isValidFor(@Nonnull DimensionInfo toValidate) {
    if (toValidate == null) {
      throw new IllegalArgumentException("DimensionInfo cannot be null");
    }
    try {
      if (!worldNames.contains(toValidate.getDimensionLevelName())) {
        return false;
      }
      getDimensionName(toValidate.getDimensionIdentifier());
      return true;
    } catch (NoSuchElementException thrown) {
      return false;
    }
  }

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
