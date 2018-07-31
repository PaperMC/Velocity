package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class LoginPluginMessage implements MinecraftPacket {
    private int id;
    private String channel;
    private ByteBuf data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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
        return "LoginPluginMessage{" +
                "id=" + id +
                ", channel='" + channel + '\'' +
                ", data=" + data +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.id = ProtocolUtils.readVarInt(buf);
        this.channel = ProtocolUtils.readString(buf);
        if (buf.isReadable()) {
            this.data = buf.readRetainedSlice(buf.readableBytes());
        } else {
            this.data = Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeVarInt(buf, id);
        ProtocolUtils.writeString(buf, channel);
        buf.writeBytes(data);
    }
}
