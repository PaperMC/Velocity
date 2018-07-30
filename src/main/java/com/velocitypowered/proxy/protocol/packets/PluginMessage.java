package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class PluginMessage implements MinecraftPacket {
    private String channel;
    private byte[] data;

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.channel = ProtocolUtils.readString(buf, 20);
        this.data = new byte[buf.readableBytes()];
        buf.readBytes(this.data);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, channel);
        buf.writeBytes(data);
    }
}
