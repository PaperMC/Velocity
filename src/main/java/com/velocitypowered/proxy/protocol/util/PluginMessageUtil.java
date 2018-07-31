package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packets.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public enum PluginMessageUtil {
    ;

    public static List<String> getChannels(PluginMessage message) {
        Preconditions.checkArgument(message.getChannel().equals("REGISTER") ||
                message.getChannel().equals("UNREGISTER") ||
                message.getChannel().equals("minecraft:register") ||
                message.getChannel().equals("minecraft:unregister"),
                "Unknown channel type " + message.getChannel());
        String channels = message.getData().toString(StandardCharsets.UTF_8);
        return ImmutableList.copyOf(channels.split("\0"));
    }

    public static PluginMessage constructChannelsPacket(String channel, Collection<String> channels) {
        PluginMessage message = new PluginMessage();
        message.setChannel(channel);

        ByteBuf data = Unpooled.buffer();
        for (String s : channels) {
            ByteBufUtil.writeUtf8(data, s);
            data.writeByte(0);
        }
        data.writerIndex(data.writerIndex() - 1);

        message.setData(data);
        return message;
    }

    public static PluginMessage rewriteMCBrand(PluginMessage message) {
        ByteBuf rewrittenBuf = Unpooled.buffer();
        String currentBrand = ProtocolUtils.readString(message.getData());
        ProtocolUtils.writeString(rewrittenBuf, currentBrand + " (Velocity)");

        PluginMessage newMsg = new PluginMessage();
        newMsg.setChannel(message.getChannel());
        newMsg.setData(rewrittenBuf);
        return newMsg;
    }
}
