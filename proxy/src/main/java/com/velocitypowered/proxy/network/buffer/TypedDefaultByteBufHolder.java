package com.velocitypowered.proxy.network.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public abstract class TypedDefaultByteBufHolder<S extends TypedDefaultByteBufHolder<S>> extends DefaultByteBufHolder {
  protected TypedDefaultByteBufHolder(final ByteBuf data) {
    super(data);
  }

  @Override
  public abstract S replace(final ByteBuf content);

  @Override
  @SuppressWarnings("unchecked")
  public S retain() {
    return (S) super.retain();
  }

  @Override
  @SuppressWarnings("unchecked")
  public S retain(final int increment) {
    return (S) super.retain(increment);
  }

  @Override
  @SuppressWarnings("unchecked")
  public S touch() {
    return (S) super.touch();
  }

  @Override
  @SuppressWarnings("unchecked")
  public S touch(final Object hint) {
    return (S) super.touch(hint);
  }
}
