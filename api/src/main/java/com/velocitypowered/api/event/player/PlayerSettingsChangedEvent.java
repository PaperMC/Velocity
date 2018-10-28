package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.PlayerSettings;

public final class PlayerSettingsChangedEvent {
    private final Player player;
    private final PlayerSettings playerSettings;

    public PlayerSettingsChangedEvent(Player player, PlayerSettings playerSettings) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.playerSettings = Preconditions.checkNotNull(playerSettings, "playerSettings");
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerSettings getPlayerSettings() {
        return playerSettings;
    }
}
