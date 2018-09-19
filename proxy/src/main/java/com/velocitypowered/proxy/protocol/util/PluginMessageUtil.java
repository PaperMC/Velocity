package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public enum PluginMessageUtil {
    ;

    public static boolean isMCBrand(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        return message.getChannel().equals("MC|Brand") || message.getChannel().equals("minecraft:brand");
    }

    public static boolean isMCRegister(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        return message.getChannel().equals("REGISTER") || message.getChannel().equals("minecraft:register");
    }

    public static boolean isMCUnregister(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        return message.getChannel().equals("UNREGISTER") || message.getChannel().equals("minecraft:unregister");
    }

    public static List<String> getChannels(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkArgument(isMCRegister(message) || isMCUnregister(message),"Unknown channel type %s",
                message.getChannel());
        String channels = new String(message.getData(), StandardCharsets.UTF_8);
        return ImmutableList.copyOf(channels.split("\0"));
    }

    public static PluginMessage constructChannelsPacket(String channel, Collection<String> channels) {
        Preconditions.checkNotNull(channel, "channel");
        Preconditions.checkNotNull(channel, "channels");

        PluginMessage message = new PluginMessage();
        message.setChannel(channel);
        message.setData(String.join("\0", channels).getBytes(StandardCharsets.UTF_8));
        return message;
    }

    public static PluginMessage rewriteMCBrand(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkArgument(isMCBrand(message), "message is not a MC Brand plugin message");

        byte[] rewrittenData;
        ByteBuf rewrittenBuf = Unpooled.buffer();
        try {
            String currentBrand = ProtocolUtils.readString(Unpooled.wrappedBuffer(message.getData()));
            ProtocolUtils.writeString(rewrittenBuf, currentBrand + " (Velocity)");
            rewrittenData = new byte[rewrittenBuf.readableBytes()];
            rewrittenBuf.readBytes(rewrittenData);
        } finally {
            rewrittenBuf.release();
        }

        PluginMessage newMsg = new PluginMessage();
        newMsg.setChannel(message.getChannel());
        newMsg.setData(rewrittenData);
        return newMsg;
    }
}
