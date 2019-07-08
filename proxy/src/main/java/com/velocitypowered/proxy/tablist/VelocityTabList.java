package com.velocitypowered.proxy.tablist;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.text.translation.TranslationManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityTabList implements TabList {

  private final VelocityServer server;
  private final ConnectedPlayer player;
  private final Map<UUID, VelocityTabListEntry> entries = new ConcurrentHashMap<>();

  private @Nullable Component header;
  private @Nullable Component footer;

  public VelocityTabList(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  /**
   * Sends packets to update translated
   * components on the client.
   */
  public void updateTranslations() {
    Locale locale = this.player.getLocale();
    TranslationManager translationManager = this.server.getTranslationManager();

    Component header = this.header;
    if (header != null) {
      header = translationManager.translateComponent(locale, header);
    }

    Component footer = this.footer;
    if (footer != null) {
      footer = translationManager.translateComponent(locale, footer);
    }

    if (header != this.header || footer != this.footer) {
      player.getMinecraftConnection().delayedWrite(HeaderAndFooter.create(header, footer));
    }

    List<PlayerListItem.Item> displayNameUpdates = null;
    for (VelocityTabListEntry entry : this.entries.values()) {
      Component displayName = entry.getDisplayName().orElse(null);
      if (displayName != null) {
        Component translated = translationManager.translateComponent(locale, displayName);
        if (displayName != translated) {
          if (displayNameUpdates == null) {
            displayNameUpdates = new ArrayList<>();
          }
          displayNameUpdates.add(itemFrom(entry, translated));
        }
      }
    }
    if (displayNameUpdates != null) {
      player.getMinecraftConnection().delayedWrite(
          new PlayerListItem(PlayerListItem.UPDATE_DISPLAY_NAME, displayNameUpdates));
    }
  }

  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
    Preconditions.checkNotNull(header, "header");
    Preconditions.checkNotNull(footer, "footer");

    this.header = header;
    this.footer = footer;

    TranslationManager translationManager = this.server.getTranslationManager();
    Locale locale = this.player.getLocale();

    Component translatedHeader = translationManager.translateComponent(locale, header);
    Component translatedFooter = translationManager.translateComponent(locale, footer);

    player.getMinecraftConnection().write(
        HeaderAndFooter.create(translatedHeader, translatedFooter));
  }

  @Override
  public void clearHeaderAndFooter() {
    header = null;
    footer = null;
    player.getMinecraftConnection().write(HeaderAndFooter.reset());
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

    PlayerListItem.Item packetItem = itemFrom(entry);
    player.getMinecraftConnection().write(
        new PlayerListItem(PlayerListItem.ADD_PLAYER, Collections.singletonList(packetItem)));
    entries.put(entry.getProfile().getId(), (VelocityTabListEntry) entry);
  }

  @Override
  public Optional<TabListEntry> removeEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");

    TabListEntry entry = entries.remove(uuid);
    if (entry != null) {
      PlayerListItem.Item packetItem = itemFrom(entry);
      player.getMinecraftConnection().write(
          new PlayerListItem(PlayerListItem.REMOVE_PLAYER, Collections.singletonList(packetItem)));
    }

    return Optional.ofNullable(entry);
  }

  @Override
  public boolean containsEntry(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return entries.containsKey(uuid);
  }

  /**
   * Clears all entries from the tab list. Note that the entries are written with
   * {@link MinecraftConnection#delayedWrite(Object)}, so make sure to do an explicit
   * {@link MinecraftConnection#flush()}.
   */
  public void clearAll() {
    List<PlayerListItem.Item> items = new ArrayList<>();
    for (TabListEntry value : entries.values()) {
      items.add(itemFrom(value));
    }
    entries.clear();
    player.getMinecraftConnection().delayedWrite(
        new PlayerListItem(PlayerListItem.REMOVE_PLAYER, items));
  }

  @Override
  public Collection<TabListEntry> getEntries() {
    return Collections.unmodifiableCollection(this.entries.values());
  }

  @Override
  public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency,
      int gameMode) {
    return new VelocityTabListEntry(this, profile, displayName, latency, gameMode);
  }

  /**
   * Processes a tab list entry packet from the backend.
   * @param packet the packet to process
   */
  public void processBackendPacket(PlayerListItem packet) {
    // Packets are already forwarded on, so no need to do that here
    for (PlayerListItem.Item item : packet.getItems()) {
      UUID uuid = item.getUuid();
      if (packet.getAction() != PlayerListItem.ADD_PLAYER && !entries.containsKey(uuid)) {
        // Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
        continue;
      }

      switch (packet.getAction()) {
        case PlayerListItem.ADD_PLAYER: {
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
        case PlayerListItem.REMOVE_PLAYER:
          entries.remove(uuid);
          break;
        case PlayerListItem.UPDATE_DISPLAY_NAME: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setDisplayNameInternal(item.getDisplayName());
          }
          break;
        }
        case PlayerListItem.UPDATE_LATENCY: {
          VelocityTabListEntry entry = entries.get(uuid);
          if (entry != null) {
            entry.setLatencyInternal(item.getLatency());
          }
          break;
        }
        case PlayerListItem.UPDATE_GAMEMODE: {
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
      PlayerListItem.Item packetItem = itemFrom(entry);
      player.getMinecraftConnection().write(
          new PlayerListItem(action, Collections.singletonList(packetItem)));
    }
  }

  PlayerListItem.Item itemFrom(TabListEntry entry) {
    Component displayName = entry.getDisplayName().orElse(null);
    if (displayName != null) {
      displayName = this.server.getTranslationManager().translateComponent(
          this.player.getLocale(), displayName);
    }
    return itemFrom(entry, displayName);
  }

  static PlayerListItem.Item itemFrom(TabListEntry entry, @Nullable Component displayName) {
    return new PlayerListItem.Item(entry.getProfile().getId())
        .setName(entry.getProfile().getName())
        .setProperties(entry.getProfile().getProperties())
        .setLatency(entry.getLatency())
        .setGameMode(entry.getGameMode())
        .setDisplayName(displayName);
  }
}
