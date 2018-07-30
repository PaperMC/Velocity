package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardTeam implements MinecraftPacket {
    private String id;
    private byte mode;

    private String displayName;
    private String prefix;
    private String suffix;
    private byte flags;
    private String nameTagVisibility;
    private String collisionRule;
    private byte color;
    private List<String> entities;

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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public String getNameTagVisibility() {
        return nameTagVisibility;
    }

    public void setNameTagVisibility(String nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
    }

    public String getCollisionRule() {
        return collisionRule;
    }

    public void setCollisionRule(String collisionRule) {
        this.collisionRule = collisionRule;
    }

    public byte getColor() {
        return color;
    }

    public void setColor(byte color) {
        this.color = color;
    }

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return "ScoreboardTeam{" +
                "id='" + id + '\'' +
                ", mode=" + mode +
                ", displayName='" + displayName + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", flags=" + flags +
                ", nameTagVisibility='" + nameTagVisibility + '\'' +
                ", collisionRule='" + collisionRule + '\'' +
                ", color=" + color +
                ", entities=" + entities +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.id = ProtocolUtils.readString(buf, 16);
        this.mode = buf.readByte();

        switch (mode) {
            case 0: // create
            case 2: // update
                this.displayName = ProtocolUtils.readString(buf);
                this.prefix = ProtocolUtils.readString(buf);
                this.suffix = ProtocolUtils.readString(buf);
                this.flags = buf.readByte();
                this.nameTagVisibility = ProtocolUtils.readString(buf, 32);
                this.collisionRule = ProtocolUtils.readString(buf, 32);
                this.color = buf.readByte();
                if (mode == 0) {
                    this.entities = readEntities(buf);
                }
                break;
            case 1: // remove
                break;
            case 3: // add player
            case 4: // remove player
                this.entities = readEntities(buf);
                break;
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, id);
        buf.writeByte(mode);
        switch (mode) {
            case 0: // create
            case 2: // update
                ProtocolUtils.writeString(buf, displayName);
                ProtocolUtils.writeString(buf, prefix);
                ProtocolUtils.writeString(buf, suffix);
                buf.writeByte(flags);
                ProtocolUtils.writeString(buf, nameTagVisibility);
                ProtocolUtils.writeString(buf, collisionRule);
                buf.writeByte(color);
                if (mode == 0) {
                    writeEntities(buf, entities);
                }
                break;
            case 1:
                break;
            case 3:
            case 4:
                writeEntities(buf, entities);
                break;
        }
    }

    private static List<String> readEntities(ByteBuf buf) {
        List<String> entities = new ArrayList<>();
        int size = ProtocolUtils.readVarInt(buf);
        for (int i = 0; i < size; i++) {
            entities.add(ProtocolUtils.readString(buf, 40));
        }
        return entities;
    }

    private static void writeEntities(ByteBuf buf, List<String> entities) {
        ProtocolUtils.writeVarInt(buf, entities.size());
        for (String entity : entities) {
            ProtocolUtils.writeString(buf, entity);
        }
    }
}
