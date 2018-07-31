package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.data.scoreboard.ObjectiveMode;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.ScoreboardProtocolUtil;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;

public class ScoreboardObjective implements MinecraftPacket {
    public static final byte ADD = (byte) 0;
    public static final byte REMOVE = (byte) 1;
    public static final byte CHANGE = (byte) 2;
    private String id;
    private byte mode;
    private Component displayName;
    private ObjectiveMode type;

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

    public Component getDisplayName() {
        return displayName;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }

    public ObjectiveMode getType() {
        return type;
    }

    public void setType(ObjectiveMode type) {
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
        if (this.mode != REMOVE) {
            this.displayName = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                this.type = ScoreboardProtocolUtil.getMode(ProtocolUtils.readVarInt(buf));
            } else {
                this.type = ScoreboardProtocolUtil.getMode(ProtocolUtils.readString(buf));
            }
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, id);
        buf.writeByte(mode);
        if (this.mode != REMOVE) {
            ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, displayName);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                ProtocolUtils.writeVarInt(buf, type.ordinal());
            } else {
                ProtocolUtils.writeString(buf, type.name().toLowerCase());
            }
        }
    }
}
