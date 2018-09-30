package com.velocitypowered.api.proxy.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * TODO: Desetude
 */
public interface TabListEntry {
    TabList getTabList();

    GameProfile getProfile();

    Optional<Component> getDisplayName();

    TabListEntry setDisplayName(@Nullable Component displayName);

    int getLatency();

    TabListEntry setLatency(int latency);

    int getGameMode();

    TabListEntry setGameMode(int gameMode);

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private TabList tabList;
        private GameProfile profile;
        private Component displayName;
        private int latency = 0;
        private int gameMode = 0;

        private Builder() {}

        public Builder tabList(TabList tabList) {
            this.tabList = tabList;
            return this;
        }

        public Builder profile(GameProfile profile) {
            this.profile = profile;
            return this;
        }

        public Builder displayName(@Nullable Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder latency(int latency) {
            this.latency = latency;
            return this;
        }

        public Builder gameMode(int gameMode) {
            this.gameMode = gameMode;
            return this;
        }

        public TabListEntry build() {
            Preconditions.checkState(tabList != null, "The Tablist must be set when building a TabListEntry");
            Preconditions.checkState(profile != null, "The GameProfile must be set when building a TabListEntry");

            return tabList.buildEntry(profile, displayName, latency, gameMode);
        }
    }
}
