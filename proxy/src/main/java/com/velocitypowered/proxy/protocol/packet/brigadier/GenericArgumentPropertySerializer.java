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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

class GenericArgumentPropertySerializer<T extends ArgumentType<?>>
    implements ArgumentPropertySerializer<T> {

  private final Supplier<T> argumentSupplier;

  private GenericArgumentPropertySerializer(Supplier<T> argumentSupplier) {
    this.argumentSupplier = argumentSupplier;
  }

  public static <T extends ArgumentType<?>> ArgumentPropertySerializer<T> create(
      Supplier<T> supplier) {
    return new GenericArgumentPropertySerializer<>(supplier);
  }

  @Override
  public @Nullable T deserialize(ByteBuf buf) {
    return argumentSupplier.get();
  }

  @Override
  public void serialize(T object, ByteBuf buf) {

  }
}
