package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundJoinGamePacket implements Packet {
  public static final PacketReader<ClientboundJoinGamePacket> DECODER = PacketReader.method(ClientboundJoinGamePacket::new);

  private static final BinaryTagIO.Reader JOINGAME_READER = BinaryTagIO.reader(2 * 1024 * 1024);
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
  private CompoundBinaryTag biomeRegistry; // 1.16.2+

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

  public CompoundBinaryTag getBiomeRegistry() {
    return biomeRegistry;
  }

  public void setBiomeRegistry(CompoundBinaryTag biomeRegistry) {
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
        + ", maxPlayers=" + maxPlayers
        + ", levelType='" + levelType + '\''
        + ", viewDistance=" + viewDistance
        + ", reducedDebugInfo=" + reducedDebugInfo
        + ", dimensionRegistry='" + dimensionRegistry + '\''
        + ", dimensionInfo='" + dimensionInfo + '\''
        + ", previousGamemode=" + previousGamemode
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    this.entityId = buf.readInt();
    if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
      this.isHardcore = buf.readBoolean();
      this.gamemode = buf.readByte();
    } else {
      this.gamemode = buf.readByte();
      this.isHardcore = (this.gamemode & 0x08) != 0;
      this.gamemode &= ~0x08;
    }
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      this.previousGamemode = buf.readByte();
      ImmutableSet<String> levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));
      CompoundBinaryTag registryContainer = ProtocolUtils.readCompoundTag(buf, JOINGAME_READER);
      ListBinaryTag dimensionRegistryContainer = null;
      if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
        dimensionRegistryContainer = registryContainer.getCompound("minecraft:dimension_type")
            .getList("value", BinaryTagTypes.COMPOUND);
        this.biomeRegistry = registryContainer.getCompound("minecraft:worldgen/biome");
      } else {
        dimensionRegistryContainer = registryContainer.getList("dimension",
            BinaryTagTypes.COMPOUND);
      }
      ImmutableSet<DimensionData> readData =
          DimensionRegistry.fromGameData(dimensionRegistryContainer, version);
      this.dimensionRegistry = new DimensionRegistry(readData, levelNames);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        CompoundBinaryTag currentDimDataTag = ProtocolUtils.readCompoundTag(buf, JOINGAME_READER);
        dimensionIdentifier = ProtocolUtils.readString(buf);
        this.currentDimensionData = DimensionData.decodeBaseCompoundTag(currentDimDataTag, version)
            .annotateWith(dimensionIdentifier, null);
      } else {
        dimensionIdentifier = ProtocolUtils.readString(buf);
        levelName = ProtocolUtils.readString(buf);
      }
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_9_1)) {
      this.dimension = buf.readInt();
    } else {
      this.dimension = buf.readByte();
    }
    if (version.lte(ProtocolVersion.MINECRAFT_1_13_2)) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      this.partialHashedSeed = buf.readLong();
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
      this.maxPlayers = ProtocolUtils.readVarInt(buf);
    } else {
      this.maxPlayers = buf.readUnsignedByte();
    }
    if (version.lt(ProtocolVersion.MINECRAFT_1_16)) {
      this.levelType = ProtocolUtils.readString(buf, 16);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_14)) {
      this.viewDistance = ProtocolUtils.readVarInt(buf);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      this.reducedDebugInfo = buf.readBoolean();
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      this.showRespawnScreen = buf.readBoolean();
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
    }
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    buf.writeInt(entityId);
    if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      buf.writeByte(previousGamemode);
      ProtocolUtils.writeStringArray(buf, dimensionRegistry.getLevelNames().toArray(new String[0]));
      CompoundBinaryTag.Builder registryContainer = CompoundBinaryTag.builder();
      ListBinaryTag encodedDimensionRegistry = dimensionRegistry.encodeRegistry(version);
      if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
        CompoundBinaryTag.Builder dimensionRegistryEntry = CompoundBinaryTag.builder();
        dimensionRegistryEntry.putString("type", "minecraft:dimension_type");
        dimensionRegistryEntry.put("value", encodedDimensionRegistry);
        registryContainer.put("minecraft:dimension_type", dimensionRegistryEntry.build());
        registryContainer.put("minecraft:worldgen/biome", biomeRegistry);
      } else {
        registryContainer.put("dimension", encodedDimensionRegistry);
      }
      ProtocolUtils.writeCompoundTag(buf, registryContainer.build());
      if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
        ProtocolUtils.writeCompoundTag(buf, currentDimensionData.serializeDimensionDetails());
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      } else {
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
        ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
      }
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_9_1)) {
      buf.writeInt(dimension);
    } else {
      buf.writeByte(dimension);
    }
    if (version.lte(ProtocolVersion.MINECRAFT_1_13_2)) {
      buf.writeByte(difficulty);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      buf.writeLong(partialHashedSeed);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_16_2)) {
      ProtocolUtils.writeVarInt(buf, maxPlayers);
    } else {
      buf.writeByte(maxPlayers);
    }
    if (version.lt(ProtocolVersion.MINECRAFT_1_16)) {
      if (levelType == null) {
        throw new IllegalStateException("No level type specified.");
      }
      ProtocolUtils.writeString(buf, levelType);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_14)) {
      ProtocolUtils.writeVarInt(buf, viewDistance);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeBoolean(reducedDebugInfo);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_15)) {
      buf.writeBoolean(showRespawnScreen);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      buf.writeBoolean(dimensionInfo.isDebugType());
      buf.writeBoolean(dimensionInfo.isFlat());
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }
}
