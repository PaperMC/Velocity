package com.velocitypowered.proxy.tablist;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityTabList implements TabList {
    private final MinecraftConnection connection;
    private final Map<UUID, TabListEntry> entries = new ConcurrentHashMap<>();

    public VelocityTabList(MinecraftConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setHeaderAndFooter(Component header, Component footer) {
        Preconditions.checkNotNull(header, "header");
        Preconditions.checkNotNull(footer, "footer");
        connection.write(HeaderAndFooter.create(header, footer));
    }

    @Override
    public void clearHeaderAndFooter() {
        connection.write(HeaderAndFooter.reset());
    }

    @Override
    public void addEntry(TabListEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        Preconditions.checkArgument(entry.getTabList().equals(this), "The provided entry was not created by this tab list");
        Preconditions.checkArgument(!entries.containsKey(entry.getProfile().idAsUuid()), "this TabList already contains an entry with the same uuid");

        PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
        connection.write(new PlayerListItem(PlayerListItem.ADD_PLAYER, Collections.singletonList(packetItem)));
        entries.put(entry.getProfile().idAsUuid(), entry);
    }

    @Override
    public Optional<TabListEntry> removeEntry(UUID uuid) {
        TabListEntry entry = entries.remove(uuid);
        if (entry != null) {
            PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
            connection.write(new PlayerListItem(PlayerListItem.REMOVE_PLAYER, Collections.singletonList(packetItem)));
        }

        return Optional.ofNullable(entry);
    }

    public void clearAll() { // Note: this method is called upon server switch
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (TabListEntry value : entries.values()) {
            items.add(PlayerListItem.Item.from(value));
        }
        entries.clear();
        connection.delayedWrite(new PlayerListItem(PlayerListItem.REMOVE_PLAYER, items));
    }

    @Override
    public Collection<TabListEntry> getEntries() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    @Override
    public TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency, int gameMode) {
        return new VelocityTabListEntry(this, profile, displayName, latency, gameMode);
    }

    public void processBackendPacket(PlayerListItem packet) {
        //Packets are already forwarded on, so no need to do that here
        for (PlayerListItem.Item item : packet.getItems()) {
            UUID uuid = item.getUuid();
            if (packet.getAction() != PlayerListItem.ADD_PLAYER && !entries.containsKey(uuid)) {
                //Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
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
                    entries.put(item.getUuid(), TabListEntry.builder()
                            .tabList(this)
                            .profile(new GameProfile(UuidUtils.toUndashed(uuid), name, properties))
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
                    TabListEntry entry = entries.get(uuid);
                    if (entry != null) {
                        entry.setDisplayName(item.getDisplayName());
                    }
                    break;
                }
                case PlayerListItem.UPDATE_LATENCY: {
                    TabListEntry entry = entries.get(uuid);
                    if (entry != null) {
                        entry.setLatency(item.getLatency());
                    }
                    break;
                }
                case PlayerListItem.UPDATE_GAMEMODE: {
                    TabListEntry entry = entries.get(uuid);
                    if (entry != null) {
                        entry.setLatency(item.getGameMode());
                    }
                    break;
                }
            }
        }
    }

    void updateEntry(int action, TabListEntry entry) {
        if (entries.containsKey(entry.getProfile().idAsUuid())) {
            PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
            connection.write(new PlayerListItem(action, Collections.singletonList(packetItem)));
        }
    }
}
