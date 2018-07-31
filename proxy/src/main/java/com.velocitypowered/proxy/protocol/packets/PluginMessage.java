package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class PluginMessage implements MinecraftPacket {
    private String channel;
    private ByteBuf data;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PluginMessage{" +
                "channel='" + channel + '\'' +
                ", data=" + ByteBufUtil.hexDump(data) +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.channel = ProtocolUtils.readString(buf);
        this.data = buf.readRetainedSlice(buf.readableBytes());
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, channel);
        buf.writeBytes(data);
    }
}
