/*
 * Copyright (C) 2020-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.util.collect;

import java.util.EnumSet;

/**
 * An immutable map of {@link Enum} entries to {@code int}s.
 *
 * @param <E> the enum type
 */
public final class Enum2IntMap<E extends Enum<E>> {

  private final int[] mappings;

  private Enum2IntMap(int[] mappings) {
    this.mappings = mappings;
  }

  public int get(E key) {
    return mappings[key.ordinal()];
  }

  /**
   * A builder for {@link Enum2IntMap}.
   *
   * @param <E> the enum type to use
   */
  public static class Builder<E extends Enum<E>> {

    private final int[] mappings;
    private final EnumSet<E> populated;
    private int defaultValue = -1;

    public Builder(Class<E> klazz) {
      this.mappings = new int[klazz.getEnumConstants().length];
      this.populated = EnumSet.noneOf(klazz);
    }

    /**
     * Adds a mapping to the map.
     *
     * @param key   the key to use
     * @param value the value to associate with the key
     * @return {@code this}, for chaining
     */
    public Builder<E> put(E key, int value) {
      this.mappings[key.ordinal()] = value;
      this.populated.add(key);
      return this;
    }

    public Builder<E> remove(E key) {
      this.populated.remove(key);
      return this;
    }

    public Builder<E> defaultValue(int defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    /**
     * Fetches a mapping from the map.
     *
     * @param key the key to use
     * @return the value in the map
     */
    public int get(E key) {
      if (this.populated.contains(key)) {
        return this.mappings[key.ordinal()];
      }
      return this.defaultValue;
    }

    /**
     * Builds the map.
     *
     * @return the built map
     */
    public Enum2IntMap<E> build() {
      for (E unpopulated : EnumSet.complementOf(this.populated)) {
        this.mappings[unpopulated.ordinal()] = this.defaultValue;
      }
      return new Enum2IntMap<>(this.mappings.clone());
    }
  }
}
