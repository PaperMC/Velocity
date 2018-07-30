package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packets.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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

    public static PluginMessage rewriteMCBrand(PluginMessage message) {
        ByteBuf currentBrandBuf = Unpooled.wrappedBuffer(message.getData());
        ByteBuf rewrittenBuf = Unpooled.buffer();
        byte[] rewrittenBrand;
        try {
            String currentBrand = ProtocolUtils.readString(currentBrandBuf);
            ProtocolUtils.writeString(rewrittenBuf, currentBrand + " (Velocity)");
            rewrittenBrand = new byte[rewrittenBuf.readableBytes()];
            rewrittenBuf.readBytes(rewrittenBrand);
        } finally {
            rewrittenBuf.release();
        }

        PluginMessage newMsg = new PluginMessage();
        newMsg.setChannel("MC|Brand");
        newMsg.setData(rewrittenBrand);
        return newMsg;
    }
}
