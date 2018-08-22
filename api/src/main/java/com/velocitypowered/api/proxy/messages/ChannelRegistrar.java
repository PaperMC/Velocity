package com.velocitypowered.api.proxy.messages;

/**
 * Represents an interface to register and unregister {@link MessageHandler} instances for handling plugin messages from
 * the client or the server.
 */
public interface ChannelRegistrar {
    void register(MessageHandler handler, ChannelIdentifier... identifiers);

    void unregister(ChannelIdentifier... identifiers);
}
