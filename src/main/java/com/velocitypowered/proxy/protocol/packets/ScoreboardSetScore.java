package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class ScoreboardSetScore implements MinecraftPacket {
    private String entity;
    private byte action;
    private String objective;
    private int value;

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ScoreboardSetScore{" +
                "entity='" + entity + '\'' +
                ", action=" + action +
                ", objective='" + objective + '\'' +
                ", value=" + value +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.entity = ProtocolUtils.readString(buf, 40);
        this.action = buf.readByte();
        this.objective = ProtocolUtils.readString(buf, 16);
        if (this.action == 0) {
            value = ProtocolUtils.readVarInt(buf);
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, entity);
        buf.writeByte(action);
        ProtocolUtils.writeString(buf, objective);
        if (this.action == 0) {
            ProtocolUtils.writeVarInt(buf, value);
        }
    }
}
