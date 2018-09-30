package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.text.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the tab list of a {@link Player}.
 * TODO: Desetude
 */
public interface TabList {
    /**
     * Sets the tab list header and footer for the player.
     * @param header the header component
     * @param footer the footer component
     */
    void setHeaderAndFooter(Component header, Component footer);

    /**
     * Clears the tab list header and footer for the player.
     */
    void clearHeaderAndFooter();

    void addEntry(TabListEntry entry);

    Optional<TabListEntry> removeEntry(UUID uuid);

    Collection<TabListEntry> getEntries();

    //Necessary because the TabListEntry implementation isn't in the api
    @Deprecated
    TabListEntry buildEntry(GameProfile profile, Component displayName, int latency, int gameMode);
}
