package com.velocitypowered.api.proxy.messages;

/**
 * Represents something that can send plugin messages.
 */
public interface ChannelMessageSink {
    /**
     * Sends a plugin message to this target.
     * @param identifier the channel identifier to send the message on
     * @param data the data to send
     */
    void sendPluginMessage(ChannelIdentifier identifier, byte[] data);
}
