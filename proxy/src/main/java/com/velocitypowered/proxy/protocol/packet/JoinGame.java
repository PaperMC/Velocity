package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.protocol.*;
import io.netty.buffer.ByteBuf;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.ListTag;
import net.kyori.nbt.TagType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JoinGame implements MinecraftPacket {

  private int entityId;
  private short gamemode;
  private int dimension;
  private long partialHashedSeed; // 1.15+
  private short difficulty;
  private boolean isHardcore;
  private int maxPlayers;
  private @Nullable String levelType;
  private int viewDistance; // 1.14+
  private boolean reducedDebugInfo;
  private boolean showRespawnScreen;
  private DimensionRegistry dimensionRegistry; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16+
  private DimensionData currentDimensionData; // 1.16.2+
  private short previousGamemode; // 1.16+
  private CompoundTag biomeRegistry; // 1.16.2+

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

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public @Nullable String getLevelType() {
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

  public short getPreviousGamemode() {
    return previousGamemode;
  }

  public void setPreviousGamemode(short previousGamemode) {
    this.previousGamemode = previousGamemode;
  }

  public boolean getIsHardcore() {
    return isHardcore;
  }

  public void setIsHardcore(boolean isHardcore) {
    this.isHardcore = isHardcore;
  }

  public CompoundTag getBiomeRegistry() {
    return biomeRegistry;
  }

  public void setBiomeRegistry(CompoundTag biomeRegistry) {
    this.biomeRegistry = biomeRegistry;
  }

  public DimensionData getCurrentDimensionData() {
    return currentDimensionData;
  }

  @Override
  public String toString() {
    return "JoinGame{"
        + "entityId=" + entityId
        + ", gamemode=" + gamemode
        + ", dimension=" + dimension
        + ", partialHashedSeed=" + partialHashedSeed
        + ", difficulty=" + difficulty
        + ", isHardcore=" + isHardcore
        + ", maxPlayers=" + maxPlayers
        + ", levelType='" + levelType + '\''
        + ", viewDistance=" + viewDistance
        + ", reducedDebugInfo=" + reducedDebugInfo
        + ", dimensionRegistry='" + dimensionRegistry.toString() + '\''
        + ", dimensionInfo='" + dimensionInfo.toString() + '\''
        + ", previousGamemode=" + previousGamemode
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.entityId = buf.readInt();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      this.isHardcore = buf.readBoolean();
      this.gamemode = buf.readByte();
    } else {
      this.gamemode = buf.readByte();
      this.isHardcore = (this.gamemode & 0x08) != 0;
      this.gamemode &= ~0x08;
    }
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      this.previousGamemode = buf.readByte();
      ImmutableSet<String> levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));
      CompoundTag registryContainer = ProtocolUtils.readCompoundTag(buf);
      ListTag dimensionRegistryContainer = null;
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        dimensionRegistryContainer = registryContainer.getCompound("minecraft:dimension_type")
                .getList("value", TagType.COMPOUND);
        this.biomeRegistry = registryContainer.getCompound("minecraft:worldgen/biome");
      } else {
        dimensionRegistryContainer = registryContainer.getList("dimension", TagType.COMPOUND);
      }
      ImmutableSet<DimensionData> readData =
              DimensionRegistry.fromGameData(dimensionRegistryContainer, version);
      this.dimensionRegistry = new DimensionRegistry(readData, levelNames);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        CompoundTag currentDimDataTag = ProtocolUtils.readCompoundTag(buf);
        dimensionIdentifier = ProtocolUtils.readString(buf);
        this.currentDimensionData = DimensionData.decodeBaseCompoundTag(currentDimDataTag, version)
            .annotateWith(dimensionIdentifier, null);
      } else {
        dimensionIdentifier = ProtocolUtils.readString(buf);
        levelName = ProtocolUtils.readString(buf);
      }
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
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      this.maxPlayers = ProtocolUtils.readVarInt(buf);
    } else {
      this.maxPlayers = buf.readUnsignedByte();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
      this.levelType = ProtocolUtils.readString(buf, 16);
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
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      buf.writeByte(previousGamemode);
      ProtocolUtils.writeStringArray(buf, dimensionRegistry.getLevelNames().toArray(
              new String[dimensionRegistry.getLevelNames().size()]));
      CompoundTag registryContainer = new CompoundTag();
      ListTag encodedDimensionRegistry = dimensionRegistry.encodeRegistry(version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        CompoundTag dimensionRegistryDummy = new CompoundTag();
        dimensionRegistryDummy.putString("type", "minecraft:dimension_type");
        dimensionRegistryDummy.put("value", encodedDimensionRegistry);
        registryContainer.put("minecraft:dimension_type", dimensionRegistryDummy);
        registryContainer.put("minecraft:worldgen/biome", biomeRegistry);
      } else {
        registryContainer.put("dimension", encodedDimensionRegistry);
      }
      ProtocolUtils.writeCompoundTag(buf, registryContainer);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        ProtocolUtils.writeCompoundTag(buf, currentDimensionData.serializeDimensionDetails());
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      } else {
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
        ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
      }
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
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      ProtocolUtils.writeVarInt(buf, maxPlayers);
    } else {
      buf.writeByte(maxPlayers);
    }
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
