package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Respawn implements MinecraftPacket {

  private int dimension;
  private long partialHashedSeed;
  private short difficulty;
  private short gamemode;
  private String levelType = "";
  private boolean shouldKeepPlayerData;
  private boolean isDebug;
  private boolean isFlat;
  private String dimensionRegistryName;

  public Respawn() {
  }

  public Respawn(int dimension, long partialHashedSeed, short difficulty, short gamemode,
      String levelType, boolean shouldKeepPlayerData, boolean isDebug, boolean isFlat, String dimensionRegistryName) {
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.levelType = levelType;
    this.shouldKeepPlayerData = shouldKeepPlayerData;
    this.isDebug = isDebug;
    this.isFlat = isFlat;
    this.dimensionRegistryName = dimensionRegistryName;
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

  public boolean getIsDebug() {
    return isDebug;
  }

  public void setIsDebug(boolean isDebug) {
    this.isDebug = isDebug;
  }

  public boolean getIsFlat() {
    return isFlat;
  }

  public void setIsFlat(boolean isFlat) {
    this.isFlat = isFlat;
  }

  public String getDimensionRegistryName() {
    return dimensionRegistryName;
  }

  public void setDimensionRegistryName(String dimensionRegistryName) {
    this.dimensionRegistryName = dimensionRegistryName;
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
        + ", isDebug=" + isDebug
        + ", isFlat='" + isFlat
        + ", dimensionRegistryName='" + dimensionRegistryName + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      this.dimensionRegistryName = ProtocolUtils.readString(buf); // Not sure what the cap on that is
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
      isDebug = buf.readBoolean();
      isFlat = buf.readBoolean();
      shouldKeepPlayerData = buf.readBoolean();
    } else {
      this.levelType = ProtocolUtils.readString(buf, 16);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      ProtocolUtils.writeString(buf, dimensionRegistryName);
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
      buf.writeBoolean(isDebug);
      buf.writeBoolean(isFlat);
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
