/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.registry;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface for retrieving packet codecs and IDs for a specific protocol version.
 */
public interface PacketRegistry {
  @Nullable
  PacketCodec<? extends MinecraftPacket> getCodec(final int id);

  <T extends MinecraftPacket> @Nullable PacketCodec<T> getCodec(
      final Class<T> packetClass);

  int getPacketId(final MinecraftPacket packet);

  boolean canDecodePacket(MinecraftPacket packet);
}
