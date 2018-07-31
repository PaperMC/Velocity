package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardTeam implements MinecraftPacket {
    private String id;
    private byte mode;

    private Component displayName;
    private Component prefix;
    private Component suffix;
    private byte flags;
    private String nameTagVisibility;
    private String collisionRule;
    private int color;
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

    public Component getDisplayName() {
        return displayName;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }

    public Component getPrefix() {
        return prefix;
    }

    public void setPrefix(Component prefix) {
        this.prefix = prefix;
    }

    public Component getSuffix() {
        return suffix;
    }

    public void setSuffix(Component suffix) {
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
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
                this.displayName = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
                if (protocolVersion <= ProtocolConstants.MINECRAFT_1_12_2) {
                    this.prefix = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
                    this.suffix = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
                }
                this.flags = buf.readByte();
                this.nameTagVisibility = ProtocolUtils.readString(buf, 32);
                this.collisionRule = ProtocolUtils.readString(buf, 32);
                this.color = protocolVersion <= ProtocolConstants.MINECRAFT_1_12_2 ? buf.readByte() :
                        ProtocolUtils.readVarInt(buf);
                if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                    this.prefix = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
                    this.suffix = ProtocolUtils.readScoreboardTextComponent(buf, protocolVersion);
                }
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
                ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, displayName);
                if (protocolVersion <= ProtocolConstants.MINECRAFT_1_12_2) {
                    ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, prefix);
                    ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, suffix);
                }
                buf.writeByte(flags);
                ProtocolUtils.writeString(buf, nameTagVisibility);
                ProtocolUtils.writeString(buf, collisionRule);
                if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                    ProtocolUtils.writeVarInt(buf, color);
                    ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, prefix);
                    ProtocolUtils.writeScoreboardTextComponent(buf, protocolVersion, suffix);
                } else {
                    buf.writeByte(color);
                }
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
