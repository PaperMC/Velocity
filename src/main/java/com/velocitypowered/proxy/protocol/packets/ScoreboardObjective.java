package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class ScoreboardObjective implements MinecraftPacket {
    private String id;
    private byte mode;
    private String displayName;
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte getMode() {
        return mode;
    }

    public void setMode(byte mode) {
        this.mode = mode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ScoreboardObjective{" +
                "id='" + id + '\'' +
                ", mode=" + mode +
                ", displayName='" + displayName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.id = ProtocolUtils.readString(buf, 16);
        this.mode = buf.readByte();
        if (this.mode != 1) {
            this.displayName = ProtocolUtils.readString(buf, 32);
            this.type = ProtocolUtils.readString(buf, 16);
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, id);
        buf.writeByte(mode);
        if (this.mode != 1) {
            ProtocolUtils.writeString(buf, displayName);
            ProtocolUtils.writeString(buf, type);
        }
    }
}
