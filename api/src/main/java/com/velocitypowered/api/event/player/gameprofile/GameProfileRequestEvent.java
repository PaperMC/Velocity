package com.velocitypowered.api.event.player.gameprofile;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;


public class GameProfileRequestEvent {
    private final String username;
    private final GameProfile originalProfile;
    private final boolean onlineMode;
    private GameProfile gameProfile;

    public GameProfileRequestEvent(GameProfile originalProfile, boolean onlinemode) {
        this.originalProfile = Preconditions.checkNotNull(originalProfile, "profile");
        this.username = originalProfile.getName();
        this.onlineMode = onlinemode;
    }

    public String getUsername() {
        return username;
    }
    
    public GameProfile getOriginalProfile() {
        return originalProfile;
    }
    
    public boolean isOnlineMode() {
        return onlineMode;
    }
    /**
     * 
     * @return a GameProfile, can be null
     */
    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(@Nullable GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    @Override
    public String toString() {
        return "GameProfileRequestEvent{"+
                    "username=" + username +
                    ", gameProfile=" + gameProfile +
                    "}";
    }
    
    
}
