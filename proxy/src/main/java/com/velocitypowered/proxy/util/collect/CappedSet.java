package com.velocitypowered.proxy.util.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An unsynchronized collection that puts an upper bound on the size of the collection.
 */
public class CappedSet<T> extends ForwardingSet<T> {

  private final Set<T> delegate;
  private final int upperSize;

  private CappedSet(Set<T> delegate, int upperSize) {
    this.delegate = delegate;
    this.upperSize = upperSize;
  }

  /**
   * Creates a capped collection backed by a {@link HashSet}.
   * @param maxSize the maximum size of the collection
   * @param <T> the type of elements in the collection
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
