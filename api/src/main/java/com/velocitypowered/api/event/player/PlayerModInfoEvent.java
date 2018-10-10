package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.ModInfo;

/**
 * This event is fired when the players ModInfo is changed.
 */
public final class PlayerModInfoEvent {
    private final Player player;
    private final ModInfo modInfo;
    
    public PlayerModInfoEvent(Player player, ModInfo modInfo) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.modInfo = Preconditions.checkNotNull(modInfo, "modInfo");
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ModInfo getModInfo() {
        return modInfo;
    }
}
