/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import static java.util.Objects.requireNonNull;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.OpaqueArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

class OpaqueArgumentTypeImpl implements OpaqueArgumentType {

  private final ArgumentIdentifier identifier;
  private final OpaqueArgumentType.PropertySerializer propSerializer;

  public OpaqueArgumentTypeImpl(final ArgumentIdentifier identifier,
                                final OpaqueArgumentType.PropertySerializer propSerializer) {
    this.identifier = requireNonNull(identifier);
    this.propSerializer = requireNonNull(propSerializer);
  }

  @Override
  public Void parse(StringReader reader) throws CommandSyntaxException {
    // Consume all the input to halt parsing by Brigadier.
    reader.setCursor(reader.getTotalLength());
    return null;
  }

  ArgumentIdentifier identifier() {
    return this.identifier;
  }

  @Nullable
  @Override
  public String getIdentifier() {
    return this.identifier.getIdentifier();
  }

  @Nullable
  @Override
  public Integer getIdentifier(final ProtocolVersion version) {
    return this.identifier.getIdByProtocolVersion(version);
  }

  @Override
  public byte[] getProperties(final ProtocolVersion version) {
    return this.propSerializer.serialize(version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpaqueArgumentTypeImpl that = (OpaqueArgumentTypeImpl) o;
    return identifier.equals(that.identifier) && propSerializer.equals(that.propSerializer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, propSerializer);
  }

  @Override
  public String toString() {
    return "OpaqueArgumentTypeImpl{" +
        "identifier=" + identifier +
        '}';
  }
}
