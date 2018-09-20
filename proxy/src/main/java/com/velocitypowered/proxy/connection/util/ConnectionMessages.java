package com.velocitypowered.proxy.connection.util;

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class ConnectionMessages {
    public static final TextComponent ALREADY_CONNECTED = TextComponent.of("You are already connected to this server!", TextColor.RED);
    public static final TextComponent IN_PROGRESS = TextComponent.of("You are already connecting to a server!", TextColor.RED);
    public static final TextComponent INTERNAL_SERVER_CONNECTION_ERROR = TextComponent.of("Internal server connection error");
    public static final TextComponent UNEXPECTED_DISCONNECT = TextComponent.of("Unexpectedly disconnected from server - crash?");

    private ConnectionMessages() {
        throw new AssertionError();
    }
}
