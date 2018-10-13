package com.velocitypowered.api.proxy.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * Represents a single entry in a {@link TabList}.
 */
public interface TabListEntry {
    /**
     * Returns the parent {@link TabList} of this {@code this} {@link TabListEntry}.
     * @return parent {@link TabList}
     */
    @NonNull TabList getTabList();
    
    /**
     * Returns the {@link GameProfile} of the entry, which uniquely identifies the entry
     * with the containing {@link java.util.UUID}, as well as deciding what is shown
     * as the player head in the tab list.
     * @return {@link GameProfile} of the entry
     */
    @NonNull GameProfile getProfile();
    
    /**
     * Returns {@link Optional} text {@link Component}, which if present is the text displayed for
     * {@code this} entry in the {@link TabList}, otherwise {@link GameProfile#getName()} is shown.
     * @return {@link Optional} text {@link Component} of name displayed in the tab list
     */
    @NonNull Optional<Component> getDisplayName();
    
    /**
     * Sets the text {@link Component} to be displayed for {@code this} {@link TabListEntry}.
     * If {@code null}, {@link GameProfile#getName()} will be shown.
     * @param displayName to show in the {@link TabList} for {@code this} entry
     * @return {@code this}, for chaining
     */
    @NonNull TabListEntry setDisplayName(@Nullable Component displayName);
    
    /**
     * Returns the latency for {@code this} entry.
     * <p>The icon shown in the tab list is calculated by the latency in the following way:<p>
     * <ul>
     *     <li>A negative latency will display the no connection icon</li>
     *     <li>0-150 will display 5 bars</li>
     *     <li>150-300 will display 4 bars</li>
     *     <li>300-600 will display 3 bars</li>
     *     <li>600-1000 will display 2 bars</li>
     *     <li>A latency move than 1 second will display 1 bar</li>
     *     <li></li>
     * </ul>
     * @return latency set for {@code this} entry
     */
    int getLatency();
    
    /**
     * Sets the latency for {@code this} entry to the specified value
     * @see #getLatency()
     * @param latency to changed to
     * @return {@code this}, for chaining
     */
    @NonNull TabListEntry setLatency(int latency);
    
    /**
     * Gets the game mode {@code this} entry has been set to.
     * <p>The number corresponds to the game mode in the following way:</p>
     * <ol start="0">
     *     <li>Survival</li>
     *     <li>Creative</li>
     *     <li>Adventure</li>
     *     <li>Spectator</li>
     * </ol>
     * @return the game mode
     */
    int getGameMode();
    
    /**
     * Sets the game mode for {@code this} entry to the specified value
     * @see #getGameMode()
     * @param gameMode to change to
     * @return {@code this}, for chaining
     */
    TabListEntry setGameMode(int gameMode);
    
    /**
     * Returns a {@link Builder} to create a {@link TabListEntry}.
     * @return {@link TabListEntry} builder
     */
    static Builder builder() {
        return new Builder();
    }
    
    /**
     * Represents a builder which creates {@link TabListEntry}s.
     * @see TabListEntry
     */
    class Builder {
        private TabList tabList;
        private GameProfile profile;
        private Component displayName;
        private int latency = 0;
        private int gameMode = 0;

        private Builder() {}
    
        /**
         * Sets the parent {@link TabList} for this entry,
         * the entry will only be able to be added to that specific {@link TabList}.
         * @param tabList to set
         * @return {@code this}, for chaining
         */
        public Builder tabList(TabList tabList) {
            this.tabList = tabList;
            return this;
        }
    
        /**
         * Sets the {@link GameProfile} of the {@link TabListEntry}.
         * @see TabListEntry#getProfile()
         * @param profile to set
         * @return {@code this}, for chaining
         */
        public Builder profile(GameProfile profile) {
            this.profile = profile;
            return this;
        }
    
        /**
         * Sets the displayed name of the {@link TabListEntry}
         * @see TabListEntry#getDisplayName()
         * @param displayName to set
         * @return {@code this}, for chaining
         */
        public Builder displayName(@Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }
    
        /**
         * Sets the latency of the {@link TabListEntry}
         * @see TabListEntry#getLatency()
         * @param latency to set
         * @return {@code this}, for chaining
         */
        public Builder latency(int latency) {
            this.latency = latency;
            return this;
        }
    
        /**
         * Sets the game mode of the {@link TabListEntry}
         * @see TabListEntry#getGameMode()
         * @param gameMode to set
         * @return {@code this}, for chaining
         */
        public Builder gameMode(int gameMode) {
            this.gameMode = gameMode;
            return this;
        }
    
        /**
         * Constructs the {@link TabListEntry} specified by {@code this} {@link Builder}.
         * @return the constructed {@link TabListEntry}
         */
        public TabListEntry build() {
            Preconditions.checkState(tabList != null, "The Tablist must be set when building a TabListEntry");
            Preconditions.checkState(profile != null, "The GameProfile must be set when building a TabListEntry");

            return tabList.buildEntry(profile, displayName, latency, gameMode);
        }
    }
}
