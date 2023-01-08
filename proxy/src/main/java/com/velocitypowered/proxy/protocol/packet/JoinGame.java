/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DimensionData;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.protocol.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JoinGame implements MinecraftPacket {

  private static final BinaryTagIO.Reader JOINGAME_READER = BinaryTagIO.reader(4 * 1024 * 1024);
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
  private int simulationDistance; // 1.18+
  private @Nullable Pair<String, Long> lastDeathPosition;
  private CompoundBinaryTag chatTypeRegistry; // placeholder, 1.19+
  private @Nullable CompoundBinaryTag originalRegistryContainerTag;

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

  public void setLevelType(@Nullable String levelType) {
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

  public int getSimulationDistance() {
    return simulationDistance;
  }

  public void setSimulationDistance(int simulationDistance) {
    this.simulationDistance = simulationDistance;
  }

  public Pair<String, Long> getLastDeathPosition() {
    return lastDeathPosition;
  }

  public void setLastDeathPosition(Pair<String, Long> lastDeathPosition) {
    this.lastDeathPosition = lastDeathPosition;
  }

  public CompoundBinaryTag getChatTypeRegistry() {
    return chatTypeRegistry;
  }

  public void setChatTypeRegistry(CompoundBinaryTag chatTypeRegistry) {
    this.chatTypeRegistry = chatTypeRegistry;
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
        + ", simulationDistance=" + simulationDistance
        + ", lastDeathPosition='" + lastDeathPosition + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
      // so separate it out.
      this.decode116Up(buf, version);
    } else {
      this.decodeLegacy(buf, version);
    }
  }

  private void decodeLegacy(ByteBuf buf, ProtocolVersion version) {
    this.entityId = buf.readInt();
    this.gamemode = buf.readByte();
    this.isHardcore = (this.gamemode & 0x08) != 0;
    this.gamemode &= ~0x08;

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9_1) >= 0) {
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
    this.levelType = ProtocolUtils.readString(buf, 16);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      this.viewDistance = ProtocolUtils.readVarInt(buf);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.reducedDebugInfo = buf.readBoolean();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      this.showRespawnScreen = buf.readBoolean();
    }
  }

  private void decode116Up(ByteBuf buf, ProtocolVersion version) {
    this.entityId = buf.readInt();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      this.isHardcore = buf.readBoolean();
      this.gamemode = buf.readByte();
    } else {
      this.gamemode = buf.readByte();
      this.isHardcore = (this.gamemode & 0x08) != 0;
      this.gamemode &= ~0x08;
    }

    this.previousGamemode = buf.readByte();

    ImmutableSet<String> levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));

    CompoundBinaryTag registryContainer = ProtocolUtils.readCompoundTag(buf, JOINGAME_READER);
    ListBinaryTag dimensionRegistryContainer;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      dimensionRegistryContainer = registryContainer.getCompound("minecraft:dimension_type")
          .getList("value", BinaryTagTypes.COMPOUND);
      this.biomeRegistry = registryContainer.getCompound("minecraft:worldgen/biome");
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
        this.chatTypeRegistry = registryContainer.getCompound("minecraft:chat_type");
      } else {
        this.chatTypeRegistry = CompoundBinaryTag.empty();
      }
    } else {
      dimensionRegistryContainer = registryContainer.getList("dimension",
          BinaryTagTypes.COMPOUND);
    }
    ImmutableSet<DimensionData> readData =
        DimensionRegistry.fromGameData(dimensionRegistryContainer, version);
    this.dimensionRegistry = new DimensionRegistry(readData, levelNames);
    this.originalRegistryContainerTag = registryContainer;

    String dimensionIdentifier;
    String levelName = null;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0
        && version.compareTo(ProtocolVersion.MINECRAFT_1_19) < 0) {
      CompoundBinaryTag currentDimDataTag = ProtocolUtils.readCompoundTag(buf, JOINGAME_READER);
      dimensionIdentifier = ProtocolUtils.readString(buf);
      this.currentDimensionData = DimensionData.decodeBaseCompoundTag(currentDimDataTag, version)
          .annotateWith(dimensionIdentifier, null);
    } else {
      dimensionIdentifier = ProtocolUtils.readString(buf);
      levelName = ProtocolUtils.readString(buf);
    }

    this.partialHashedSeed = buf.readLong();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      this.maxPlayers = ProtocolUtils.readVarInt(buf);
    } else {
      this.maxPlayers = buf.readUnsignedByte();
    }

    this.viewDistance = ProtocolUtils.readVarInt(buf);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_18) >= 0) {
      this.simulationDistance = ProtocolUtils.readVarInt(buf);
    }

    this.reducedDebugInfo = buf.readBoolean();
    this.showRespawnScreen = buf.readBoolean();
    boolean isDebug = buf.readBoolean();
    boolean isFlat = buf.readBoolean();
    this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
    // optional death location
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0 && buf.readBoolean()) {
      this.lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
      // so separate it out.
      this.encode116Up(buf, version);
    } else {
      this.encodeLegacy(buf, version);
    }
  }

  private void encodeLegacy(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9_1) >= 0) {
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
    if (levelType == null) {
      throw new IllegalStateException("No level type specified.");
    }
    ProtocolUtils.writeString(buf, levelType);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_14) >= 0) {
      ProtocolUtils.writeVarInt(buf, viewDistance);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      buf.writeBoolean(reducedDebugInfo);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      buf.writeBoolean(showRespawnScreen);
    }
  }

  private void encode116Up(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    buf.writeByte(previousGamemode);
    ProtocolUtils.writeStringArray(buf, dimensionRegistry.getLevelNames().toArray(new String[0]));
    if (this.originalRegistryContainerTag != null) {
      ProtocolUtils.writeCompoundTag(buf, this.originalRegistryContainerTag);
    } else {
      CompoundBinaryTag.Builder registryContainer = CompoundBinaryTag.builder();
      ListBinaryTag encodedDimensionRegistry = dimensionRegistry.encodeRegistry(version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
        CompoundBinaryTag.Builder dimensionRegistryEntry = CompoundBinaryTag.builder();
        dimensionRegistryEntry.putString("type", "minecraft:dimension_type");
        dimensionRegistryEntry.put("value", encodedDimensionRegistry);
        registryContainer.put("minecraft:dimension_type", dimensionRegistryEntry.build());
        registryContainer.put("minecraft:worldgen/biome", biomeRegistry);
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
          registryContainer.put("minecraft:chat_type", chatTypeRegistry);
        }
      } else {
        registryContainer.put("dimension", encodedDimensionRegistry);
      }
      ProtocolUtils.writeCompoundTag(buf, registryContainer.build());
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0
        && version.compareTo(ProtocolVersion.MINECRAFT_1_19) < 0) {
      ProtocolUtils.writeCompoundTag(buf, currentDimensionData.serializeDimensionDetails());
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
    } else {
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
    }
    buf.writeLong(partialHashedSeed);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
      ProtocolUtils.writeVarInt(buf, maxPlayers);
    } else {
      buf.writeByte(maxPlayers);
    }
    ProtocolUtils.writeVarInt(buf, viewDistance);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_18) >= 0) {
      ProtocolUtils.writeVarInt(buf, simulationDistance);
    }

    buf.writeBoolean(reducedDebugInfo);
    buf.writeBoolean(showRespawnScreen);
    buf.writeBoolean(dimensionInfo.isDebugType());
    buf.writeBoolean(dimensionInfo.isFlat());

    // optional death location
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (lastDeathPosition != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeString(buf, lastDeathPosition.key());
        buf.writeLong(lastDeathPosition.value());
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
