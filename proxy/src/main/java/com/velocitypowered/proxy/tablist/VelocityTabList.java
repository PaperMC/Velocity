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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VelocityTabList implements TabList {
    private final MinecraftConnection connection;
    private final Map<UUID, TabListEntry> entries = new HashMap<>();

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
        connection.write(new PlayerListItem(PlayerListItem.Action.ADD_PLAYER, Collections.singletonList(packetItem)));
        entries.put(entry.getProfile().idAsUuid(), entry);
    }

    @Override
    public Optional<TabListEntry> removeEntry(UUID uuid) {
        TabListEntry entry = entries.remove(uuid);
        if (entry != null) {
            PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
            connection.write(new PlayerListItem(PlayerListItem.Action.REMOVE_PLAYER, Collections.singletonList(packetItem)));
        }

        return Optional.ofNullable(entry);
    }

    @Override
    public Collection<TabListEntry> getEntries() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    @Override
    public TabListEntry buildEntry(GameProfile profile, Component displayName, int latency, int gameMode) {
        return new VelocityTabListEntry(this, profile, displayName, latency, gameMode);
    }

    public void processBackendPacket(PlayerListItem packet) {
        //Packets are already forwarded on, so no need to do that here
        for (PlayerListItem.Item item : packet.getItems()) {
            UUID uuid = item.getUuid();
            if (packet.getAction() != PlayerListItem.Action.ADD_PLAYER && !entries.containsKey(uuid)) {
                //Sometimes UPDATE_GAMEMODE is sent before ADD_PLAYER so don't want to warn here
                continue;
            }

            switch (packet.getAction()) {
                case ADD_PLAYER:
                    entries.put(item.getUuid(), TabListEntry.builder()
                            .tabList(this)
                            .profile(new GameProfile(UuidUtils.toUndashed(uuid), item.getName(), item.getProperties()))
                            .displayName(item.getDisplayName())
                            .latency(item.getLatency())
                            .gameMode(item.getGameMode())
                            .build());
                    break;
                case REMOVE_PLAYER:
                    entries.remove(uuid);
                    break;
                case UPDATE_DISPLAY_NAME:
                    entries.get(uuid).setDisplayName(item.getDisplayName());
                    break;
                case UPDATE_LATENCY:
                    entries.get(uuid).setLatency(item.getLatency());
                    break;
                case UPDATE_GAMEMODE:
                    entries.get(uuid).setGameMode(item.getGameMode());
                    break;
            }
        }
    }

    void updateEntry(PlayerListItem.Action action, TabListEntry entry) {
        if (entries.containsKey(entry.getProfile().idAsUuid())) {
            PlayerListItem.Item packetItem = PlayerListItem.Item.from(entry);
            connection.write(new PlayerListItem(action, Collections.singletonList(packetItem)));
        }
    }
}
