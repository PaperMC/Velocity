package com.velocitypowered.api.proxy.messages;

/**
 * Represents from "which side" of the proxy the plugin message came from.
 */
public enum ChannelSide {
    /**
     * The plugin message came from a server that a client was connected to.
     */
    FROM_SERVER,
    /**
     * The plugin message came from the client.
     */
    FROM_CLIENT
}
