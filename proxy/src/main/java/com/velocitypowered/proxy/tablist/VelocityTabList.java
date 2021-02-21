package com.velocitypowered.proxy.tablist;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundHeaderAndFooterPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPlayerListItemPacket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabList implements TabList {

  protected final ConnectedPlayer player;
  protected final MinecraftConnection connection;
  protected final Map<UUID, VelocityTabListEntry> entries = new ConcurrentHashMap<>();

  public VelocityTabList(final ConnectedPlayer player) {
    this.player = player;
    this.connection = player.getConnection();
  }

  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
    Preconditions.checkNotNull(header, "header");
    Preconditions.checkNotNull(footer, "footer");
    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(
        connection.getProtocolVersion());
    connection.write(new ClientboundHeaderAndFooterPacket(
        serializer.serialize(header),
        serializer.serialize(footer)
    ));
  }

  @Override
  public void clearHeaderAndFooter() {
    connection.write(ClientboundHeaderAndFooterPacket.reset());
  }

  @Override
  public void addEntry(TabListEntry entry) {
    Preconditions.checkNotNull(entry, "entry");
    Preconditions.checkArgument(entry.getTabList().equals(this),
        "The provided entry was not created by this tab list");
    Preconditions.checkArgument(!entries.containsKey(entry.getProfile().getId()),
        "this TabList already contains an entry with the same uuid");
    Preconditions.checkArgument(entry instanceof VelocityTabListEntry,
        "Not a Velocity tab list entry");

    connection.write(new ClientboundPlayerListItemPacket(ClientboundPlayerListItemPacket.ADD_PLAYER,
        Collections.singletonList(ClientboundPlayerListItemPacket.Item.from(entry))));
    entries.put(entry.getProfile().getId(), (VelocityTabListEntry) entry);
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");

    TabListEntry entry = entries.remove(uuid);
    if (entry != null) {
      connection.write(new ClientboundPlayerListItemPacket(
          ClientboundPlayerListItemPacket.REMOVE_PLAYER,
          Collections.singletonList(ClientboundPlayerListItemPacket.Item.from(entry))
      ));
    }

    return Optional.ofNullable(entry);
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return entries.containsKey(uuid);
  }

  /**
   * Clears all entries from the tab list. Note that the entries are written with {@link
   * MinecraftConnection#delayedWrite(Object)}, so make sure to do an explicit {@link
   * MinecraftConnection#flush()}.
   */
  public void clearAll() {
    Collection<VelocityTabListEntry> listEntries = entries.values();
    if (listEntries.isEmpty()) {
      return;
    }
    List<ClientboundPlayerListItemPacket.Item> items = new ArrayList<>(listEntries.size());
    for (TabListEntry value : listEntries) {
      items.add(ClientboundPlayerListItemPacket.Item.from(value));
    }
    entries.clear();
    connection.delayedWrite(new ClientboundPlayerListItemPacket(
        ClientboundPlayerListItemPacket.REMOVE_PLAYER, items));
  }

  @Override
  public Collection<TabListEntry> getEntries() {
    return Collections.unmodifiableCollection(this.entries.values());
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile,
      net.kyori.adventure.text.@Nullable Component displayName, int latency, int gameMode) {
    return new VelocityTabListEntry(this, profile, displayName, latency, gameMode);
  }

  /**
   * Processes a tab list entry packet from the backend.
   *
   * @param packet the packet to process
   */
  public void processBackendPacket(ClientboundPlayerListItemPacket packet) {
    // Packets are already forwarded on, so no need to do that here
    for (ClientboundPlayerListItemPacket.Item item : packet.getItems()) {
      UUID uuid = item.getUuid();
      assert uuid != null : "1.7 tab list entry given to modern tab list handler!";

      if (packet.getAction() != ClientboundPlayerListItemPacket.ADD_PLAYER
          && !entries.containsKey(uuid)) {
        // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
        continue;
      }

      switch (packet.getAction()) {
        case ClientboundPlayerListItemPacket.ADD_PLAYER: {
          // ensure that name and properties are available
          String name = item.getName();
          List<GameProfile.Property> properties = item.getProperties();
          if (name == null || properties == null) {
            throw new IllegalStateException("Got null game profile for ADD_PLAYER");
          }
          entries.put(item.getUuid(), (VelocityTabListEntry) TabListEntry.builder()
              .tabList(this)
              .profile(new GameProfile(uuid, name, properties))
              .displayName(item.getDisplayName())
              .latency(item.getLatency())
              .gameMode(item.getGameMode())
              .build());
          break;
        }
        case ClientboundPlayerListItemPacket.REMOVE_PLAYER:
          entries.remove(uuid);
          break;
        case ClientboundPlayerListItemPacket.UPDATE_DISPLAY_NAME: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setDisplayNameInternal(item.getDisplayName());
          }
          break;
        }
        case ClientboundPlayerListItemPacket.UPDATE_LATENCY: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
          break;
        }
        case ClientboundPlayerListItemPacket.UPDATE_GAMEMODE: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setGameModeInternal(item.getGameMode());
          }
          break;
        }
        default:
          // Nothing we can do here
          break;
      }
    }
  }

  void updateEntry(int action, TabListEntry entry) {
    if (entries.containsKey(entry.getProfile().getId())) {
      connection.write(new ClientboundPlayerListItemPacket(action,
          Collections.singletonList(ClientboundPlayerListItemPacket.Item.from(entry))));
    }
  }
}
