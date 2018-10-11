package com.velocitypowered.api.command;

public enum PermissionState {

    /**
     * Represents a command which can only be run by the console.
     */
    CONSOLE_ONLY,

    /**
     * Represents a command which can only be run by a player.
     */
    PLAYER_ONLY,

    /**
     * Represents a command which can be run by both the console and players.
     */
    PERMISSIVE,
    ;
}
