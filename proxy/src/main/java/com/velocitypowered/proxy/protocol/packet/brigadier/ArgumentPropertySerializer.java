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

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The {@code ArgumentPropertySerializer} interface defines a contract for serializing and
 * deserializing argument properties to and from a specific format.
 *
 * <p>This interface allows implementations to convert argument properties into a serialized form,
 * which can later be deserialized and restored to their original form. This is particularly useful
 * for persisting command argument configurations or sending them across a network.</p>
 *
 * @param <T> the type of the argument property being serialized
 */
public interface ArgumentPropertySerializer<T> {

  @Nullable T deserialize(ByteBuf buf, ProtocolVersion protocolVersion);

  void serialize(T object, ByteBuf buf, ProtocolVersion protocolVersion);
}
