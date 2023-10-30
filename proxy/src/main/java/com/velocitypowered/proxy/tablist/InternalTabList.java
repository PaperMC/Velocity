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

package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;

/**
 * Tab list interface with methods for handling player info packets.
 */
public interface InternalTabList extends TabList {

  default void processLegacy(LegacyPlayerListItem packet) {
  }

  default void processUpdate(UpsertPlayerInfo infoPacket) {
  }

  default void processRemove(RemovePlayerInfo infoPacket) {
  }

  void clearAllSilent();
}
