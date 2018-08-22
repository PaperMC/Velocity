package com.velocitypowered.api.proxy.messages;

public interface MessageHandler {
    ForwardStatus handle(ChannelMessageSource source, ChannelSide side, ChannelIdentifier identifier, byte[] data);

    enum ForwardStatus {
        FORWARD,
        HANDLED
    }
}
