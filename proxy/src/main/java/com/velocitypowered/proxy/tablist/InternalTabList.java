package com.velocitypowered.proxy.tablist;

import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;

public interface InternalTabList extends TabList {
  default void processLegacy(LegacyPlayerListItem packet) {
  }

  default void processUpdate(UpsertPlayerInfo infoPacket) {
  }

  default void processRemove(RemovePlayerInfo infoPacket) {
  }
}
