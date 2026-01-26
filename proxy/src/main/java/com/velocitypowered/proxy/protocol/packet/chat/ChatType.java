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

package com.velocitypowered.proxy.protocol.packet.chat;

/**
 * Represents different types of chat messages in the game, defining how a message should be
 * handled and displayed.
 * This enum categorizes various chat message types such as system messages, player chat,
 * or game info.
 */
public enum ChatType {
  CHAT((byte) 0),
  SYSTEM((byte) 1),
  GAME_INFO((byte) 2);

  private final byte raw;

  ChatType(byte raw) {
    this.raw = raw;
  }

  public byte getId() {
    return raw;
  }
}
