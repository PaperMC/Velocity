package com.velocitypowered.api.proxy.messages;

/**
 * Represents an interface to register and unregister {@link MessageHandler} instances for handling plugin messages from
 * the client or the server.
 */
public interface ChannelRegistrar {
    /**
     * Registers the specified message handler to listen for plugin messages on the specified channels.
     * @param handler the handler to register
     * @param identifiers the channel identifiers to register
     */
    void register(MessageHandler handler, ChannelIdentifier... identifiers);

    /**
     * Unregisters the handler for the specified channel.
     * @param identifiers the identifiers to unregister
     */
    void unregister(ChannelIdentifier... identifiers);
}
