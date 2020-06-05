package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.DimensionInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Respawn implements MinecraftPacket {

  private int dimension;
  private long partialHashedSeed;
  private short difficulty;
  private short gamemode;
  private String levelType = "";
  private boolean shouldKeepPlayerData; // 1.16+
  private DimensionInfo dimensionInfo;

  public Respawn() {
  }

  public Respawn(int dimension, long partialHashedSeed, short difficulty, short gamemode,
      String levelType, boolean shouldKeepPlayerData, DimensionInfo dimensionInfo) {
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.levelType = levelType;
    this.shouldKeepPlayerData = shouldKeepPlayerData;
    this.dimensionInfo = dimensionInfo;
  }

  public int getDimension() {
    return dimension;
  }

  public void setDimension(int dimension) {
    this.dimension = dimension;
  }

  public long getPartialHashedSeed() {
    return partialHashedSeed;
  }

  public void setPartialHashedSeed(long partialHashedSeed) {
    this.partialHashedSeed = partialHashedSeed;
  }

  public short getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(short difficulty) {
    this.difficulty = difficulty;
  }

  public short getGamemode() {
    return gamemode;
  }

  public void setGamemode(short gamemode) {
    this.gamemode = gamemode;
  }

  public String getLevelType() {
    return levelType;
  }

  public void setLevelType(String levelType) {
    this.levelType = levelType;
  }

  public boolean getShouldKeepPlayerData() {
    return shouldKeepPlayerData;
  }

  public void setShouldKeepPlayerData(boolean shouldKeepPlayerData) {
    this.shouldKeepPlayerData = shouldKeepPlayerData;
  }

  @Override
  public String toString() {
    return "Respawn{"
        + "dimension=" + dimension
        + ", partialHashedSeed=" + partialHashedSeed
        + ", difficulty=" + difficulty
        + ", gamemode=" + gamemode
        + ", levelType='" + levelType + '\''
        + ", shouldKeepPlayerData=" + shouldKeepPlayerData
        + ", dimensionRegistryName='" + dimensionInfo.toString() + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      dimensionIdentifier = ProtocolUtils.readString(buf);
      levelName = ProtocolUtils.readString(buf);
    } else {
      this.dimension = buf.readInt();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      this.partialHashedSeed = buf.readLong();
    }
    this.gamemode = buf.readUnsignedByte();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
      shouldKeepPlayerData = buf.readBoolean();
    } else {
      this.levelType = ProtocolUtils.readString(buf, 16);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      ProtocolUtils.writeString(buf, dimensionInfo.getDimensionIdentifier());
      ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
    } else {
      buf.writeInt(dimension);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      buf.writeByte(difficulty);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeLong(partialHashedSeed);
    }
    buf.writeByte(gamemode);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      buf.writeBoolean(dimensionInfo.isDebugType());
      buf.writeBoolean(dimensionInfo.isFlat());
      buf.writeBoolean(shouldKeepPlayerData);
    } else {
      ProtocolUtils.writeString(buf, levelType);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
