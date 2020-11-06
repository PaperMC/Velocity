package com.velocitypowered.proxy.tablist;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.PlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.PlayerListItemPacket.Item;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabListLegacy extends VelocityTabList {

  private final Map<String, UUID> nameMapping = new ConcurrentHashMap<>();

  public VelocityTabListLegacy(MinecraftConnection connection) {
    super(connection);
  }

  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
  }

  @Override
  public void clearHeaderAndFooter() {
  }

  @Override
  public void addEntry(TabListEntry entry) {
    super.addEntry(entry);
    nameMapping.put(entry.getProfile().getName(), entry.getProfile().getId());
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Optional<TabListEntry> entry = super.removeEntry(uuid);
    entry.map(TabListEntry::getProfile).map(GameProfile::getName).ifPresent(nameMapping::remove);
    return entry;
  }

  @Override
  public void clearAll() {
    for (TabListEntry value : entries.values()) {
      connection.delayedWrite(new PlayerListItemPacket(PlayerListItemPacket.REMOVE_PLAYER,
          Collections.singletonList(PlayerListItemPacket.Item.from(value))));
    }
    entries.clear();
    nameMapping.clear();
  }

  @Override
  public void processBackendPacket(PlayerListItemPacket packet) {
    Item item = packet.getItems().get(0); // Only one item per packet in 1.7

    switch (packet.getAction()) {
      case PlayerListItemPacket.ADD_PLAYER:
        if (nameMapping.containsKey(item.getName())) { // ADD_PLAYER also used for updating ping
          VelocityTabListEntry entry = entries.get(nameMapping.get(item.getName()));
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
        } else {
          UUID uuid = UUID.randomUUID(); // Use a fake uuid to preserve function of custom entries
          nameMapping.put(item.getName(), uuid);
          entries.put(uuid, (VelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, item.getName(), ImmutableList.of()))
              .latency(item.getLatency())
              .build());
        }
        break;
      case PlayerListItemPacket.REMOVE_PLAYER:
        UUID removedUuid = nameMapping.remove(item.getName());
        if (removedUuid != null) {
          entries.remove(removedUuid);
        }
        break;
      default:
        // For 1.7 there is only add and remove
        break;
    }

  }

  @Override
  void updateEntry(int action, TabListEntry entry) {
    if (entries.containsKey(entry.getProfile().getId())) {
      switch (action) {
        case PlayerListItemPacket.UPDATE_LATENCY:
        case PlayerListItemPacket.UPDATE_DISPLAY_NAME: // Add here because we removed beforehand
          connection
              // ADD_PLAYER also updates ping
              .write(new PlayerListItemPacket(PlayerListItemPacket.ADD_PLAYER,
                  Collections.singletonList(PlayerListItemPacket.Item.from(entry))));
          break;
        default:
          // Can't do anything else
          break;
      }
    }
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
      int gameMode) {
    return new VelocityTabListEntryLegacy(this, profile, displayName, latency, gameMode);
  }
}
