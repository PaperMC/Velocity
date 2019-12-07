package com.velocitypowered.proxy.tablist;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem.Item;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.text.Component;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
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
      connection.delayedWrite(new PlayerListItem(PlayerListItem.REMOVE_PLAYER,
          Collections.singletonList(PlayerListItem.Item.from(value))));
    }
    entries.clear();
    nameMapping.clear();
  }

  @Override
  public void processBackendPacket(PlayerListItem packet) {

    Item item = packet.getItems().get(0); // Only one item per packet in 1.7

    Component displayName = LegacyComponentSerializer.legacy().deserialize(item.getName());
    String strippedName = PlainComponentSerializer.INSTANCE.serialize(displayName);

    switch (packet.getAction()) {
      case PlayerListItem.ADD_PLAYER:
        if (nameMapping.containsKey(strippedName)) { // ADD_PLAYER also used for updating ping
          VelocityTabListEntry entry = entries.get(nameMapping.get(strippedName));
          if (entry != null) {
            entry.setLatency(item.getLatency());
          }
        } else {
          UUID uuid = UUID.randomUUID(); // Use a fake uuid to preserve function of custom entries
          nameMapping.put(strippedName, uuid);
          entries.put(uuid, (VelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, strippedName, ImmutableList.of()))
              .displayName(displayName)
              .latency(item.getLatency())
              .build());
        }
        break;
      case PlayerListItem.REMOVE_PLAYER:
        UUID removedUuid = nameMapping.remove(strippedName);
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
        case PlayerListItem.UPDATE_LATENCY:
        case PlayerListItem.UPDATE_DISPLAY_NAME: // Add here because we removed beforehand
          // ADD_PLAYER also updates ping
          connection.write(new PlayerListItem(PlayerListItem.ADD_PLAYER,
                  Collections.singletonList(PlayerListItem.Item.from(entry))));
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
