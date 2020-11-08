package com.velocitypowered.proxy.network.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.IllegalReferenceCountException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A special-purpose implementation of {@code ByteBufHolder} that can defer accepting its buffer.
 * This is required because Velocity packets are, for better or worse, mutable.
 */
public class DeferredByteBufHolder implements ByteBufHolder {

  @MonotonicNonNull
  private ByteBuf backing;

  public DeferredByteBufHolder(
      @MonotonicNonNull ByteBuf backing) {
    this.backing = backing;
  }

  @Override
  public ByteBuf content() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    if (backing.refCnt() <= 0) {
      throw new IllegalReferenceCountException(backing.refCnt());
    }
    return backing;
  }

  @Override
  public ByteBufHolder copy() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.copy());
  }

  @Override
  public ByteBufHolder duplicate() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.duplicate());
  }

  @Override
  public ByteBufHolder retainedDuplicate() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return new DeferredByteBufHolder(backing.retainedDuplicate());
  }

  @Override
  public ByteBufHolder replace(ByteBuf content) {
    if (content == null) {
      throw new NullPointerException("content");
    }
    this.backing = content;
    return this;
  }

  @Override
  public int refCnt() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.refCnt();
  }

  @Override
  public ByteBufHolder retain() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.retain();
    return this;
  }

  @Override
  public ByteBufHolder retain(int increment) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.retain(increment);
    return this;
  }

  @Override
  public ByteBufHolder touch() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.touch();
    return this;
  }

  @Override
  public ByteBufHolder touch(Object hint) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    backing.touch(hint);
    return this;
  }

  @Override
  public boolean release() {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.release();
  }

  @Override
  public boolean release(int decrement) {
    if (backing == null) {
      throw new IllegalStateException("Trying to obtain contents of holder with a null buffer");
    }
    return backing.release(decrement);
  }

  @Override
  public String toString() {
    String str = "DeferredByteBufHolder[";
    if (backing == null) {
      str += "null";
    } else {
      str += backing.toString();
    }
    return str + "]";
  }
}
