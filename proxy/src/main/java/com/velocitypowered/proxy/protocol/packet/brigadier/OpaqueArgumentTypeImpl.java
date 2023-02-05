/*
 * Copyright (C) 2021-2023 Velocity Contributors
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.OpaqueArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Objects;
import javax.annotation.Nullable;

public final class OpaqueArgumentTypeImpl implements OpaqueArgumentType {

  public static @Nullable OpaqueArgumentType from(final String identifier) {
    final ArgumentIdentifier id = ArgumentPropertyRegistry.getOldIdentifier(identifier);
    if (id != null) {
      return new OpaqueArgumentTypeImpl(id);
    }
    return null;
  }

  public static @Nullable OpaqueArgumentType from(final ProtocolVersion version, final int identifier) {
    final ArgumentIdentifier id = ArgumentPropertyRegistry.getNewIdentifier(version, identifier);
    if (id != null) {
      return new OpaqueArgumentTypeImpl(id);
    }
    return null;
  }

  private final ArgumentIdentifier identifier;

  private OpaqueArgumentTypeImpl(final ArgumentIdentifier identifier) {
    this.identifier = identifier;
  }

  @Override
  public Void parse(StringReader reader) throws CommandSyntaxException {
    // Consume all the input to halt parsing by Brigadier.
    reader.setCursor(reader.getTotalLength());
    return null;
  }

  ArgumentIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpaqueArgumentTypeImpl that = (OpaqueArgumentTypeImpl) o;
    return Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }

  @Override
  public String toString() {
    return "OpaqueArgumentTypeImpl{" + identifier + '}';
  }
}
