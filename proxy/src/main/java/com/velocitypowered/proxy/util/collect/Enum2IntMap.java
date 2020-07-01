package com.velocitypowered.proxy.util.collect;

import java.util.EnumSet;

public class Enum2IntMap<E extends Enum<E>> {
  private final int[] mappings;

  private Enum2IntMap(int[] mappings) {
    this.mappings = mappings;
  }

  public int get(E key) {
    return mappings[key.ordinal()];
  }

  public static class Builder<E extends Enum<E>> {
    private final int[] mappings;
    private final EnumSet<E> populated;
    private int defaultValue = -1;

    public Builder(Class<E> klazz) {
      this.mappings = new int[klazz.getEnumConstants().length];
      this.populated = EnumSet.noneOf(klazz);
    }

    public Builder<E> put(E key, int value) {
      this.mappings[key.ordinal()] = value;
      this.populated.add(key);
      return this;
    }

    public Builder<E> remove(E key, int value) {
      this.populated.remove(key);
      return this;
    }

    public Builder<E> defaultValue(int defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public int get(E key) {
      if (this.populated.contains(key)) {
        return this.mappings[key.ordinal()];
      }
      return this.defaultValue;
    }

    public Enum2IntMap<E> build() {
      for (E unpopulated : EnumSet.complementOf(this.populated)) {
        this.mappings[unpopulated.ordinal()] = this.defaultValue;
      }
      return new Enum2IntMap<>(this.mappings.clone());
    }
  }
}
