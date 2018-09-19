package com.velocitypowered.api.proxy.messages;

/**
 * Represents an interface to register and unregister {@link ChannelIdentifier}s for the proxy to listen on.
 */
public interface ChannelRegistrar {
    /**
     * Registers the specified message identifiers to listen on for the
     * @param identifiers the channel identifiers to register
     */
    void register(ChannelIdentifier... identifiers);

    /**
     * Unregisters the handler for the specified channel.
     * @param identifiers the identifiers to unregister
     */
    void unregister(ChannelIdentifier... identifiers);
}
