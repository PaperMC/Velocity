package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.CompoundBinaryTag;

public class RespawnPacket implements Packet {
  public static final Decoder<RespawnPacket> DECODER = Decoder.method(RespawnPacket::new);

  private int dimension;
  private long partialHashedSeed;
  private short difficulty;
  private short gamemode;
  private String levelType = "";
  private boolean shouldKeepPlayerData; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16-1.16.1
  private short previousGamemode; // 1.16+
  private DimensionData currentDimensionData; // 1.16.2+

  public RespawnPacket() {
  }

  public RespawnPacket(int dimension, long partialHashedSeed, short difficulty, short gamemode,
                       String levelType, boolean shouldKeepPlayerData, DimensionInfo dimensionInfo,
                       short previousGamemode, DimensionData currentDimensionData) {
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.levelType = levelType;
    this.shouldKeepPlayerData = shouldKeepPlayerData;
    this.dimensionInfo = dimensionInfo;
    this.previousGamemode = previousGamemode;
    this.currentDimensionData = currentDimensionData;
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

  public short getPreviousGamemode() {
    return previousGamemode;
  }

  public void setPreviousGamemode(short previousGamemode) {
    this.previousGamemode = previousGamemode;
  }

  @Override
  public String toString() {
    return "RespawnPacket{"
        + "dimension=" + dimension
        + ", partialHashedSeed=" + partialHashedSeed
        + ", difficulty=" + difficulty
        + ", gamemode=" + gamemode
        + ", levelType='" + levelType + '\''
        + ", shouldKeepPlayerData=" + shouldKeepPlayerData
        + ", dimensionRegistryName='" + dimensionInfo.toString() + '\''
        + ", dimensionInfo=" + dimensionInfo
        + ", previousGamemode=" + previousGamemode
        + ", dimensionData=" + currentDimensionData
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
        CompoundBinaryTag dimDataTag = ProtocolUtils.readCompoundTag(buf);
        dimensionIdentifier = ProtocolUtils.readString(buf);
        this.currentDimensionData = DimensionData.decodeBaseCompoundTag(dimDataTag, version)
            .annotateWith(dimensionIdentifier, null);
      } else {
        dimensionIdentifier = ProtocolUtils.readString(buf);
        levelName = ProtocolUtils.readString(buf);
      }
    } else {
      this.dimension = buf.readInt();
    }
    if (version.lte(ProtocolVersion.MINECRAFT_1_13_2)) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      this.partialHashedSeed = buf.readLong();
    }
    this.gamemode = buf.readByte();
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      this.previousGamemode = buf.readByte();
      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
      this.shouldKeepPlayerData = buf.readBoolean();
    } else {
      this.levelType = ProtocolUtils.readString(buf, 16);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
        ProtocolUtils.writeCompoundTag(buf, currentDimensionData.serializeDimensionDetails());
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      } else {
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
        ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
      }
    } else {
      buf.writeInt(dimension);
    }
    if (version.lte(ProtocolVersion.MINECRAFT_1_13_2)) {
      buf.writeByte(difficulty);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      buf.writeLong(partialHashedSeed);
    }
    buf.writeByte(gamemode);
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      buf.writeByte(previousGamemode);
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
