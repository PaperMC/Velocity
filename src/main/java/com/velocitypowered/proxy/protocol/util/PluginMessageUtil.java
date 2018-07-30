package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.protocol.packets.PluginMessage;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public enum PluginMessageUtil {
    ;

    public static List<String> getChannels(PluginMessage message) {
        Preconditions.checkArgument(message.getChannel().equals("REGISTER") ||
                message.getChannel().equals("UNREGISTER"), "Unknown channel type " + message.getChannel());
        return ImmutableList.copyOf(new String(message.getData()).split("\0"));
    }

    public static PluginMessage constructChannelsPacket(String channel, Collection<String> channels) {
        PluginMessage message = new PluginMessage();
        message.setChannel(channel);
        message.setData(Joiner.on("\0").join(channels).getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
