package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class JoinGame implements MinecraftPacket {
    private int entityId;
    private short gamemode;
    private int dimension;
    private short difficulty;
    private short maxPlayers;
    private String levelType;
    private boolean reducedDebugInfo;

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public short getGamemode() {
        return gamemode;
    }

    public void setGamemode(short gamemode) {
        this.gamemode = gamemode;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public short getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(short difficulty) {
        this.difficulty = difficulty;
    }

    public short getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(short maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getLevelType() {
        return levelType;
    }

    public void setLevelType(String levelType) {
        this.levelType = levelType;
    }

    public boolean isReducedDebugInfo() {
        return reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Override
    public String toString() {
        return "JoinGame{" +
                "entityId=" + entityId +
                ", gamemode=" + gamemode +
                ", dimension=" + dimension +
                ", difficulty=" + difficulty +
                ", maxPlayers=" + maxPlayers +
                ", levelType='" + levelType + '\'' +
                ", reducedDebugInfo=" + reducedDebugInfo +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.entityId = buf.readInt();
        this.gamemode = buf.readUnsignedByte();
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_9_1) {
            this.dimension = buf.readInt();
        } else {
            this.dimension = buf.readByte();
        }
        this.difficulty = buf.readUnsignedByte();
        this.maxPlayers = buf.readUnsignedByte();
        this.levelType = ProtocolUtils.readString(buf, 16);
        this.reducedDebugInfo = buf.readBoolean();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        buf.writeInt(entityId);
        buf.writeByte(gamemode);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_9_1) {
            buf.writeInt(dimension);
        } else {
            buf.writeByte(dimension);
        }
        buf.writeByte(difficulty);
        buf.writeByte(maxPlayers);
        ProtocolUtils.writeString(buf, levelType);
        buf.writeBoolean(reducedDebugInfo);
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
