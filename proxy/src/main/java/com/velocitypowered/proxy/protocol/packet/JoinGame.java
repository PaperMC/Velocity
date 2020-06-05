package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.protocol.*;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JoinGame implements MinecraftPacket {

  private int entityId;
  private short gamemode;
  private int dimension;
  private long partialHashedSeed; // 1.15+
  private short difficulty;
  private short maxPlayers;
  private @Nullable String levelType;
  private int viewDistance; // 1.14+
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private DimensionRegistry dimensionRegistry; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16+

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

  public long getPartialHashedSeed() {
    return partialHashedSeed;
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
    if (levelType == null) {
      throw new IllegalStateException("No level type specified.");
    }
    return levelType;
  }

  public void setLevelType(String levelType) {
    this.levelType = levelType;
  }

  public int getViewDistance() {
    return viewDistance;
  }

  public void setViewDistance(int viewDistance) {
    this.viewDistance = viewDistance;
  }

  public boolean isReducedDebugInfo() {
    return reducedDebugInfo;
  }

  public void setReducedDebugInfo(boolean reducedDebugInfo) {
    this.reducedDebugInfo = reducedDebugInfo;
  }

  public DimensionInfo getDimensionInfo() {
    return dimensionInfo;
  }

  public void setDimensionInfo(DimensionInfo dimensionInfo) {
    this.dimensionInfo = dimensionInfo;
  }

  public DimensionRegistry getDimensionRegistry() {
    return dimensionRegistry;
  }

  public void setDimensionRegistry(DimensionRegistry dimensionRegistry) {
    this.dimensionRegistry = dimensionRegistry;
  }

  @Override
  public String toString() {
    return "JoinGame{"
        + "entityId=" + entityId
        + ", gamemode=" + gamemode
        + ", dimension=" + dimension
        + ", partialHashedSeed=" + partialHashedSeed
        + ", difficulty=" + difficulty
        + ", maxPlayers=" + maxPlayers
        + ", levelType='" + levelType + '\''
        + ", viewDistance=" + viewDistance
        + ", reducedDebugInfo=" + reducedDebugInfo
        + ", dimensionRegistry='" + dimensionRegistry.toString() + '\''
        + ", dimensionInfo='" + dimensionInfo.toString() + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.entityId = buf.readInt();
    this.gamemode = buf.readUnsignedByte();
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      String levelNames[] = ProtocolUtils.readStringArray(buf);
      this.dimensionRegistry = DimensionRegistry.fromGameData(ProtocolUtils.readCompoundTag(buf), levelNames);
      dimensionIdentifier = ProtocolUtils.readString(buf);
      levelName = ProtocolUtils.readString(buf);
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_9_1) >= 0) {
      this.dimension = buf.readInt();
    } else {
      this.dimension = buf.readByte();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      this.partialHashedSeed = buf.readLong();
    }
    this.maxPlayers = buf.readUnsignedByte();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
      this.levelType = ProtocolUtils.readString(buf, 16);
    } else {
      this.levelType = "default"; // I didn't have the courage to rework this yet.
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      this.viewDistance = ProtocolUtils.readVarInt(buf);
    }
    this.reducedDebugInfo = buf.readBoolean();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      this.showRespawnScreen = buf.readBoolean();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(entityId);
    buf.writeByte(gamemode);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      ProtocolUtils.writeStringArray(buf, dimensionRegistry.getLevelNames());
      ProtocolUtils.writeCompoundTag(buf, dimensionRegistry.encodeRegistry());
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
    } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_9_1) >= 0) {
      buf.writeInt(dimension);
    } else {
      buf.writeByte(dimension);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      buf.writeByte(difficulty);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeLong(partialHashedSeed);
    }
    buf.writeByte(maxPlayers);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
      if (levelType == null) {
        throw new IllegalStateException("No level type specified.");
      }
      ProtocolUtils.writeString(buf, levelType);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      ProtocolUtils.writeVarInt(buf,viewDistance);
    }
    buf.writeBoolean(reducedDebugInfo);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeBoolean(showRespawnScreen);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      buf.writeBoolean(dimensionInfo.isDebugType());
      buf.writeBoolean(dimensionInfo.isFlat());
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
