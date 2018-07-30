package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class BossBar implements MinecraftPacket {
    private UUID uuid;
    private int action;
    private String title;
    private float health;
    private int color;
    private int divisions;
    private short flags;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getDivisions() {
        return divisions;
    }

    public void setDivisions(int divisions) {
        this.divisions = divisions;
    }

    public short getFlags() {
        return flags;
    }

    public void setFlags(short flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "BossBar{" +
                "uuid=" + uuid +
                ", action=" + action +
                ", title='" + title + '\'' +
                ", health=" + health +
                ", color=" + color +
                ", divisions=" + divisions +
                ", flags=" + flags +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.uuid = ProtocolUtils.readUuid(buf);
        this.action = ProtocolUtils.readVarInt(buf);
        switch (action) {
            case 0: // add
                this.title = ProtocolUtils.readString(buf);
                this.health = buf.readFloat();
                this.color = ProtocolUtils.readVarInt(buf);
                this.divisions = ProtocolUtils.readVarInt(buf);
                this.flags = buf.readUnsignedByte();
                break;
            case 1: // remove
                break;
            case 2: // set health
                this.health = buf.readFloat();
                break;
            case 3: // update title
                this.title = ProtocolUtils.readString(buf);
                break;
            case 4: // update style
                this.color = ProtocolUtils.readVarInt(buf);
                this.divisions = ProtocolUtils.readVarInt(buf);
                break;
            case 5:
                this.flags = buf.readUnsignedByte();
                break;
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeUuid(buf, uuid);
        ProtocolUtils.writeVarInt(buf, action);
        switch (action) {
            case 0: // add
                ProtocolUtils.writeString(buf, title);
                buf.writeFloat(health);
                ProtocolUtils.writeVarInt(buf, color);
                ProtocolUtils.writeVarInt(buf, divisions);
                buf.writeByte(flags);
                break;
            case 1: // remove
                break;
            case 2: // set health
                buf.writeFloat(health);
                break;
            case 3: // update title
                ProtocolUtils.writeString(buf, title);
                break;
            case 4: // update style
                ProtocolUtils.writeVarInt(buf, color);
                ProtocolUtils.writeVarInt(buf, divisions);
                break;
            case 5:
                buf.writeByte(flags);
                break;
        }
    }
}
