package com.velocitypowered.api.event.player.gameprofile;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;


public class GameProfileRequestEvent {
    private final String username;
    private GameProfile gameProfile;

    public GameProfileRequestEvent(String username) {
        this.username = Preconditions.checkNotNull(username, "username");
    }

    public String getUsername() {
        return username;
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
