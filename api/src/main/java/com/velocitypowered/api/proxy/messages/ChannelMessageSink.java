package com.velocitypowered.api.proxy.messages;

public interface ChannelMessageSink {
    void sendPluginMessage(ChannelIdentifier identifier, byte[] data);
}
