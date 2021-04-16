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
