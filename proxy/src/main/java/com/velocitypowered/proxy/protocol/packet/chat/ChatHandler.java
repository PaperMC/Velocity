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
 * Represents a handler for processing chat-related packets in the game.
 * This interface is generic and can handle different types of Minecraft packets that
 * extend {@link MinecraftPacket}.
 *
 * @param <T> the type of packet that this chat handler processes, which must
 *            extend {@link MinecraftPacket}
 */
public interface ChatHandler<T extends MinecraftPacket> {

  Class<T> packetClass();

  void handlePlayerChatInternal(T packet);

  /**
   * Handles a player chat event represented by the given {@link MinecraftPacket}.
   * This default method provides a basic mechanism for processing chat-related packets that
   * involve player messages.
   *
   * @param packet the {@link MinecraftPacket} representing the player chat event to handle
   * @return {@code true} if the chat event was successfully handled, {@code false} otherwise
   */
  default boolean handlePlayerChat(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerChatInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }
}
