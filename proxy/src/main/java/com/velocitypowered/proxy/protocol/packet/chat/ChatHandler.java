/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.protocol.MinecraftPacket;

/**
 * Handles inbound player chat packets of a specific type.
 *
 * @param <T> the chat packet type handled by this handler
 */
public interface ChatHandler<T extends MinecraftPacket> {

  Class<T> packetClass();

  void handlePlayerChatInternal(T packet);

  /**
   * Handles the given packet if it matches this handler's packet type.
   *
   * @param packet the packet to handle
   * @return {@code true} if the packet was handled by this handler
   */
  default boolean handlePlayerChat(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerChatInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }
}
