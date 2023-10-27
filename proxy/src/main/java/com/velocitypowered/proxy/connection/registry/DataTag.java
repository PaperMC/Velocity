/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.registry;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a data tag.
 */
public class DataTag {
  private final ImmutableList<DataTag.Set> entrySets;

  public DataTag(ImmutableList<DataTag.Set> entrySets) {
    this.entrySets = entrySets;
  }

  /**
   * Returns the entry sets.
   *
   * @return List of entry sets
   */
  public List<Set> getEntrySets() {
    return entrySets;
  }

  /**
   * Represents a data tag set.
   */
  public static class Set implements Keyed {

    private final Key key;
    private final ImmutableList<Entry> entries;

    public Set(Key key, ImmutableList<Entry> entries) {
      this.key = key;
      this.entries = entries;
    }

    /**
     * Returns the entries.
     *
     * @return List of entries
     */
    public List<Entry> getEntries() {
      return entries;
    }

    @Override
    public @NotNull Key key() {
      return key;
    }
  }

  /**
   * Represents a data tag entry.
   */
  public static class Entry implements Keyed {

    private final Key key;
    private final int[] elements;

    public Entry(Key key, int[] elements) {
      this.key = key;
      this.elements = elements;
    }

    public int[] getElements() {
      return elements;
    }

    @Override
    public @NotNull Key key() {
      return key;
    }
  }
}
