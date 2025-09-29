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

package com.velocitypowered.proxy.util.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An unsynchronized collection that puts an upper bound on the size of the collection.
 */
public final class CappedSet<T> extends ForwardingSet<T> {

  private final Set<T> delegate;
  private final int upperSize;

  private CappedSet(Set<T> delegate, int upperSize) {
    this.delegate = delegate;
    this.upperSize = upperSize;
  }

  /**
   * Creates a capped collection backed by a {@link HashSet}.
   *
   * @param maxSize the maximum size of the collection
   * @param <T>     the type of elements in the collection
   * @return the new collection
   */
  public static <T> Set<T> create(int maxSize) {
    return new CappedSet<>(new HashSet<>(), maxSize);
  }

  @Override
  protected Set<T> delegate() {
    return delegate;
  }

  @Override
  public boolean add(T element) {
    if (this.delegate.size() >= upperSize) {
      Preconditions.checkState(this.delegate.contains(element),
          "collection is too large (%s >= %s)",
          this.delegate.size(), this.upperSize);
      return false;
    }
    return this.delegate.add(element);
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    return this.standardAddAll(collection);
  }
}
