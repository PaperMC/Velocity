/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

class PassthroughProperty<T> implements ArgumentType<T> {

  private final ArgumentIdentifier identifier;
  private final ArgumentPropertySerializer<T> serializer;
  private final @Nullable T result;

  PassthroughProperty(ArgumentIdentifier identifier, ArgumentPropertySerializer<T> serializer,
      @Nullable T result) {
    this.identifier = identifier;
    this.serializer = serializer;
    this.result = result;
  }

  public ArgumentIdentifier getIdentifier() {
    return identifier;
  }

  public ArgumentPropertySerializer<T> getSerializer() {
    return serializer;
  }

  public @Nullable T getResult() {
    return result;
  }

  @Override
  public T parse(StringReader reader) {
    throw new UnsupportedOperationException();
  }
}
