package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the tab list of a {@link Player}.
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
    
    /**
     * Adds a {@link TabListEntry} to the {@link Player}'s tab list.
     * @param entry to add to the tab list
     */
    void addEntry(TabListEntry entry);
    
    /**
     * Removes the {@link TabListEntry} from the tab list with the {@link GameProfile}
     * identified with the specified {@link UUID}.
     * @param uuid of the
     * @return {@link Optional} containing the removed {@link TabListEntry} if present,
     * otherwise {@link Optional#empty()}
     */
    Optional<TabListEntry> removeEntry(UUID uuid);
    
    /**
     * Returns an immutable {@link Collection} of the {@link TabListEntry}s in the tab list.
     * @return immutable {@link Collection} of tab list entries
     */
    Collection<TabListEntry> getEntries();

    // Necessary because the TabListEntry implementation isn't in the api
    @Deprecated
    TabListEntry buildEntry(GameProfile profile, @Nullable Component displayName, int latency, int gameMode);
}
