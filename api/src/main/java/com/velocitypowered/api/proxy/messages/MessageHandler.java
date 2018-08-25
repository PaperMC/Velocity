package com.velocitypowered.api.proxy.messages;

/**
 * Represents a handler for handling plugin messages.
 */
public interface MessageHandler {
    /**
     * Handles an incoming plugin message.
     * @param source the source of the plugin message
     * @param side from where the plugin message originated
     * @param identifier the channel on which the message was sent
     * @param data the data inside the plugin message
     * @return a {@link ForwardStatus} indicating whether or not to forward this plugin message on
     */
    ForwardStatus handle(ChannelMessageSource source, ChannelSide side, ChannelIdentifier identifier, byte[] data);

    enum ForwardStatus {
        /**
         * Forwards this plugin message on to the client or server, depending on the {@link ChannelSide} it originated
         * from.
         */
        FORWARD,
        /**
         * Discard the plugin message and do not forward it on.
         */
        HANDLED
    }
}
