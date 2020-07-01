package com.velocitypowered.proxy.util.collect.concurrent;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;

/* package */ final class SyncMapImpl<K, V> extends AbstractMap<K, V> implements SyncMap<K, V> {

  private final Object lock = new Object();
  private final Function<Integer, Map<K, ExpungingValue<V>>> function;
  private volatile Map<K, ExpungingValue<V>> read;
  private volatile boolean readAmended;
  private int readMisses;
  private Map<K, ExpungingValue<V>> dirty;
  private EntrySet entrySet;

  /* package */ SyncMapImpl(final Function<Integer, Map<K, ExpungingValue<V>>> function,
      final int initialCapacity) {
    this.function = function;
    this.read = function.apply(initialCapacity);
  }

  @Override
  public int size() {
    if (this.readAmended) {
      synchronized (this.lock) {
        if (this.readAmended) {
          return this.getSize(this.dirty);
        }
      }
    }
    return this.getSize(this.read);
  }

  private int getSize(Map<K, ExpungingValue<V>> map) {
    int size = 0;
    for (ExpungingValue<V> value : map.values()) {
      if (value.exists()) {
        size++;
      }
    }
    return size;
  }

  private ExpungingValue<V> getValue(final Object key) {
    ExpungingValue<V> entry = this.read.get(key);
    if (entry == null && this.readAmended) {
      entry = this.getValueReadMissed(key);
    }
    return entry;
  }

  private ExpungingValue<V> getValueReadMissed(final Object key) {
    ExpungingValue<V> entry = null;
    synchronized (this.lock) {
      if (this.readAmended && (entry = this.read.get(key)) == null && this.dirty != null) {
        entry = this.dirty.get(key);
        this.missLocked();
      }
    }
    return entry;
  }

  @Override
  public boolean containsKey(final Object key) {
    ExpungingValue<V> entry = this.getValue(key);
    return entry != null && entry.exists();
  }

  @Override
  public V get(final Object key) {
    ExpungingValue<V> entry = this.getValue(key);
    if (entry == null) {
      return null;
    }
    return entry.get();
  }

  @Override
  public V put(final K key, final V value) {
    Objects.requireNonNull(value, "value");

    ExpungingValue<V> entry = this.read.get(key);
    V previous = entry != null ? entry.get() : null;
    if (entry != null && entry.trySet(value)) {
      return previous;
    }
    return this.putDirty(key, value, false);
  }

  private V putDirty(final K key, final V value, boolean onlyIfExists) {
    ExpungingValue<V> entry;
    V previous = null;
    synchronized (this.lock) {
      if (!onlyIfExists) {
        entry = this.read.get(key);
        if (entry != null && entry.tryUnexpungeAndSet(value)) {
          // If we had an expunged entry, then this.dirty != null and we need to insert the entry there too.
          this.dirty.put(key, entry);
          return null;
        }
      }

      if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
        previous = entry.set(value);
      } else if (!onlyIfExists) {
        if (!this.readAmended) {
          this.dirtyLocked();
          this.readAmended = true;
        }
        assert this.dirty != null;
        this.dirty.put(key, new ExpungingValueImpl<>(value));
        previous = null;
      }
    }
    return previous;
  }

  @Override
  public V remove(final Object key) {
    ExpungingValue<V> entry = this.read.get(key);
    if (entry == null && this.readAmended) {
      synchronized (this.lock) {
        if (this.readAmended && (entry = this.read.get(key)) == null && this.dirty != null) {
          entry = this.dirty.remove(key);
        }
      }
    }
    return entry != null ? entry.clear() : null;
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    Objects.requireNonNull(value, "value");

    ExpungingValue<V> entry = this.read.get(key);
    boolean absent = entry == null;
    if (absent && this.readAmended) {
      synchronized (this.lock) {
        if (this.readAmended && (absent = (entry = this.read.get(key)) == null)
            && this.dirty != null) {
          absent = (entry = this.dirty.get(key)) == null;
          if (!absent && entry.replace(value, null)) {
            this.dirty.remove(key);
            return true;
          }
        }
      }
    }
    if (!absent) {
      entry.replace(value, null);
    }
    return false;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    Objects.requireNonNull(value, "value");

    // Go in for a clean hit if we can.
    ExpungingValue<V> entry = this.read.get(key);
    if (entry != null) {
      Entry<Boolean, V> result = entry.putIfAbsent(value);
      if (result.getKey() == Boolean.TRUE) {
        return result.getValue();
      }
    }

    synchronized (this.lock) {
      entry = this.read.get(key);
      if (entry != null && entry.tryUnexpungeAndSet(value)) {
        this.dirty.put(key, entry);
        return null;
      } else if (this.dirty != null && (entry = this.dirty.get(key)) != null) {
        Entry<Boolean, V> result = entry.putIfAbsent(value);
        this.missLocked();

        // The only time an entry would be expunged is if it were in the read map, and we've already checked for
        // that earlier.
        assert result.getKey() == Boolean.TRUE;
        return result.getValue();
      } else {
        if (!this.readAmended) {
          this.dirtyLocked();
          this.readAmended = true;
        }
        assert this.dirty != null;
        this.dirty.put(key, new ExpungingValueImpl<>(value));
        return null;
      }
    }
  }

  @Override
  public V replace(K key, V value) {
    Objects.requireNonNull(value, "value");

    ExpungingValue<V> entry = this.read.get(key);
    V previous = entry != null ? entry.get() : null;
    if (entry != null && entry.trySet(value)) {
      return previous;
    }
    return this.putDirty(key, value, true);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    Objects.requireNonNull(oldValue, "oldValue");
    Objects.requireNonNull(newValue, "newValue");

    // Try a clean hit
    ExpungingValue<V> entry = this.read.get(key);
    if (entry != null && entry.replace(oldValue, newValue)) {
      return true;
    }

    // Failed, go to the slow path. This is considerably simpler than the others that need to consider expunging.
    synchronized (this.lock) {
      if (this.dirty != null) {
        entry = this.dirty.get(key);
        if (entry.replace(oldValue, newValue)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public void clear() {
    synchronized (this.lock) {
      this.read = this.function.apply(16);
      this.dirty = null;
      this.readMisses = 0;
      this.readAmended = false;
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (this.entrySet != null) {
      return this.entrySet;
    }
    return this.entrySet = new EntrySet();
  }

  private void promoteIfNeeded() {
    if (this.readAmended) {
      synchronized (this.lock) {
        if (this.readAmended && this.dirty != null) {
          this.promoteLocked();
        }
      }
    }
  }

  private void promoteLocked() {
    if (this.dirty != null) {
      this.read = this.dirty;
    }
    this.dirty = null;
    this.readMisses = 0;
    this.readAmended = false;
  }

  private void missLocked() {
    this.readMisses++;
    int length = this.dirty != null ? this.dirty.size() : 0;
    if (this.readMisses > length) {
      this.promoteLocked();
    }
  }

  private void dirtyLocked() {
    if (this.dirty == null) {
      this.dirty = this.function.apply(this.read.size());
      for (final Entry<K, ExpungingValue<V>> entry : this.read.entrySet()) {
        if (!entry.getValue().tryMarkExpunged()) {
          this.dirty.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static class ExpungingValueImpl<V> implements SyncMap.ExpungingValue<V> {

    /**
     * A marker object used to indicate that the value in this map has been expunged.
     */
    private static final Object EXPUNGED = new Object();

    // The raw type is required here, which is sad, but type erasure has forced our hand in this
    // regard. (Besides, using an Object type and casting to the desired value allows us to reuse
    // this field to see if the value has been expunged.)
    //
    // Type-safety is ensured by ensuring the special EXPUNGED value is never returned and using
    // generics on the set and constructor calls.
    private static final AtomicReferenceFieldUpdater<ExpungingValueImpl, Object> valueUpdater =
        AtomicReferenceFieldUpdater.newUpdater(ExpungingValueImpl.class, Object.class, "value");

    private volatile Object value;

    private ExpungingValueImpl(final V value) {
      this.value = value;
    }

    @Override
    public V get() {
      Object value = valueUpdater.get(this);
      if (value == EXPUNGED) {
        return null;
      }
      return (V) value;
    }

    @Override
    public Entry<Boolean, V> putIfAbsent(V value) {
      for (; ; ) {
        Object existingVal = valueUpdater.get(this);
        if (existingVal == EXPUNGED) {
          return new SimpleImmutableEntry<>(Boolean.FALSE, null);
        }

        if (existingVal != null) {
          return new SimpleImmutableEntry<>(Boolean.TRUE, (V) existingVal);
        }

        if (valueUpdater.compareAndSet(this, null, value)) {
          return new SimpleImmutableEntry<>(Boolean.TRUE, null);
        }
      }
    }

    @Override
    public boolean isExpunged() {
      return valueUpdater.get(this) == EXPUNGED;
    }

    @Override
    public boolean exists() {
      Object val = valueUpdater.get(this);
      return val != null && val != EXPUNGED;
    }

    @Override
    public V set(final V value) {
      Object oldValue = valueUpdater.getAndSet(this, value);
      return oldValue == EXPUNGED ? null : (V) oldValue;
    }

    @Override
    public boolean trySet(final V newValue) {
      for (; ; ) {
        Object foundValue = valueUpdater.get(this);
        if (foundValue == EXPUNGED) {
          return false;
        }

        if (valueUpdater.compareAndSet(this, foundValue, newValue)) {
          return true;
        }
      }
    }

    @Override
    public boolean tryMarkExpunged() {
      Object val = valueUpdater.get(this);
      while (val == null) {
        if (valueUpdater.compareAndSet(this, null, EXPUNGED)) {
          return true;
        }
        val = valueUpdater.get(this);
      }
      return false;
    }

    @Override
    public boolean tryUnexpungeAndSet(V element) {
      return valueUpdater.compareAndSet(this, EXPUNGED, element);
    }

    @Override
    public boolean replace(final Object expected, final V newValue) {
      for (; ; ) {
        Object val = valueUpdater.get(this);
        if (val == EXPUNGED || !Objects.equals(val, expected)) {
          return false;
        }

        if (valueUpdater.compareAndSet(this, val, newValue)) {
          return true;
        }
      }
    }

    @Override
    public V clear() {
      for (; ; ) {
        Object val = valueUpdater.get(this);
        if (val == null || val == EXPUNGED) {
          return null;
        }
        if (valueUpdater.compareAndSet(this, val, null)) {
          return (V) val;
        }
      }
    }
  }

  private class MapEntry implements Entry<K, V> {

    private final K key;

    private MapEntry(final Entry<K, ExpungingValue<V>> entry) {
      this.key = entry.getKey();
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public V getValue() {
      return SyncMapImpl.this.get(this.key);
    }

    @Override
    public V setValue(final V value) {
      return SyncMapImpl.this.put(this.key, value);
    }

    @Override
    public String toString() {
      return "SyncMapImpl.MapEntry{key=" + this.getKey() + ", value=" + this.getValue() + "}";
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Map.Entry)) {
        return false;
      }
      final Entry<?, ?> that = (Entry<?, ?>) other;
      return Objects.equals(this.getKey(), that.getKey())
          && Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.getKey(), this.getValue());
    }
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {

    @Override
    public int size() {
      return SyncMapImpl.this.size();
    }

    @Override
    public boolean contains(final Object entry) {
      if (!(entry instanceof Map.Entry)) {
        return false;
      }
      final Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      final V value = SyncMapImpl.this.get(mapEntry.getKey());
      return value != null && Objects.equals(mapEntry.getValue(), value);
    }

    @Override
    public boolean remove(final Object entry) {
      if (!(entry instanceof Map.Entry)) {
        return false;
      }
      final Entry<?, ?> mapEntry = (Entry<?, ?>) entry;
      return SyncMapImpl.this.remove(mapEntry.getKey()) != null;
    }

    @Override
    public void clear() {
      SyncMapImpl.this.clear();
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      SyncMapImpl.this.promoteIfNeeded();
      return new EntryIterator(SyncMapImpl.this.read.entrySet().iterator());
    }
  }

  private class EntryIterator implements Iterator<Entry<K, V>> {

    private final Iterator<Entry<K, ExpungingValue<V>>> backingIterator;
    private Entry<K, V> next;
    private Entry<K, V> current;

    private EntryIterator(final Iterator<Entry<K, ExpungingValue<V>>> backingIterator) {
      this.backingIterator = backingIterator;
      Entry<K, ExpungingValue<V>> entry = this.getNextValue();
      this.next = (entry != null ? new MapEntry(entry) : null);
    }

    private Entry<K, ExpungingValue<V>> getNextValue() {
      Entry<K, ExpungingValue<V>> entry = null;
      while (this.backingIterator.hasNext() && entry == null) {
        final ExpungingValue<V> value = (entry = this.backingIterator.next()).getValue();
        if (!value.exists()) {
          entry = null;
        }
      }
      return entry;
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Entry<K, V> next() {
      this.current = this.next;
      Entry<K, ExpungingValue<V>> entry = this.getNextValue();
      this.next = (entry != null ? new MapEntry(entry) : null);
      if (this.current == null) {
        throw new NoSuchElementException();
      }
      return this.current;
    }

    @Override
    public void remove() {
      if (this.current == null) {
        return;
      }
      SyncMapImpl.this.remove(this.current.getKey());
    }

    @Override
    public void forEachRemaining(final Consumer<? super Entry<K, V>> action) {
      if (this.next != null) {
        action.accept(this.next);
      }
      this.backingIterator.forEachRemaining(entry -> {
        if (entry.getValue().exists()) {
          action.accept(new MapEntry(entry));
        }
      });
    }
  }
}
