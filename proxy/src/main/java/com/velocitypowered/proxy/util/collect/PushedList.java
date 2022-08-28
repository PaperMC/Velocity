/*
 * Copyright (C) 2018 Velocity Contributors
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PushedList<T> extends ArrayList<T> {

  private final int indexRange;
  private int actualIndex = 0;

  public PushedList(int indexRange) {
    this.indexRange = indexRange;
  }

  public int getIndexRange() {
    return indexRange;
  }

  public int getActualSize() {
    return super.size();
  }

  public int getActualIndex() {
    return actualIndex;
  }

  public boolean offsetIndex(int step) {
    int result = step + actualIndex;
    if (result >= getActualSize() || result < 0) {
      return false;
    }
    actualIndex = result;
    return true;
  }

  public int getActualZero(){
    return Math.max(0, actualIndex - indexRange);
  }

  private List<T> internalRange(){
    return super.subList(getActualZero(), actualIndex);
  }

  @Override
  public int size() {
    return Math.min(super.size(), indexRange);
  }

  @Override
  public boolean add(T t) {
    actualIndex++;
    return super.add(t);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    actualIndex += c.size();
    return super.addAll(c);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return internalRange().iterator();
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator() {
    return internalRange().listIterator();
  }

  @NotNull
  @Override
  public ListIterator<T> listIterator(int index) {
    return internalRange().listIterator(index);
  }
}
