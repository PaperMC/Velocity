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
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JoinGamePacket implements MinecraftPacket {

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
  private boolean doLimitedCrafting; // 1.20.2+
  private ImmutableSet<String> levelNames; // 1.16+
  private CompoundBinaryTag registry; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16+
  private CompoundBinaryTag currentDimensionData; // 1.16.2+
  private short previousGamemode; // 1.16+
  private int simulationDistance; // 1.18+
  private @Nullable Pair<String, Long> lastDeathPosition; // 1.19+
  private int portalCooldown; // 1.20+
  private int seaLevel; // 1.21.2+
  private boolean enforcesSecureChat; // 1.20.5+

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

  public boolean getDoLimitedCrafting() {
    return doLimitedCrafting;
  }

  public void setDoLimitedCrafting(boolean doLimitedCrafting) {
    this.doLimitedCrafting = doLimitedCrafting;
  }

  public CompoundBinaryTag getCurrentDimensionData() {
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

  public int getPortalCooldown() {
    return portalCooldown;
  }

  public void setPortalCooldown(int portalCooldown) {
    this.portalCooldown = portalCooldown;
  }

  public int getSeaLevel() {
    return seaLevel;
  }

  public void setSeaLevel(int seaLevel) {
    this.seaLevel = seaLevel;
  }

  public boolean getEnforcesSecureChat() {
    return this.enforcesSecureChat;
  }

  public void setEnforcesSecureChat(final boolean enforcesSecureChat) {
    this.enforcesSecureChat = enforcesSecureChat;
  }

  public CompoundBinaryTag getRegistry() {
    return registry;
  }

  @Override
  public String toString() {
    return "JoinGame{" + "entityId=" + entityId + ", gamemode=" + gamemode + ", dimension=" +
        dimension + ", partialHashedSeed=" + partialHashedSeed + ", difficulty=" + difficulty +
        ", isHardcore=" + isHardcore + ", maxPlayers=" + maxPlayers + ", levelType='" + levelType +
        '\'' + ", viewDistance=" + viewDistance + ", reducedDebugInfo=" + reducedDebugInfo +
        ", showRespawnScreen=" + showRespawnScreen + ", doLimitedCrafting=" + doLimitedCrafting +
        ", levelNames=" + levelNames + ", registry='" + registry + '\'' + ", dimensionInfo='" +
        dimensionInfo + '\'' + ", currentDimensionData='" + currentDimensionData + '\'' +
        ", previousGamemode=" + previousGamemode + ", simulationDistance=" + simulationDistance +
        ", lastDeathPosition='" + lastDeathPosition + '\'' + ", portalCooldown=" + portalCooldown +
        ", seaLevel=" + seaLevel +
        '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      // haha funny, they made 1.20.2 more complicated
      this.decode1202Up(buf, version);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
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

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9_1)) {
      this.dimension = buf.readInt();
    } else {
      this.dimension = buf.readByte();
    }
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
      this.partialHashedSeed = buf.readLong();
    }
    this.maxPlayers = buf.readUnsignedByte();
    this.levelType = ProtocolUtils.readString(buf, 16);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      this.viewDistance = ProtocolUtils.readVarInt(buf);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      this.reducedDebugInfo = buf.readBoolean();
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
      this.showRespawnScreen = buf.readBoolean();
    }
  }

  private void decode116Up(ByteBuf buf, ProtocolVersion version) {
    this.entityId = buf.readInt();
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      this.isHardcore = buf.readBoolean();
      this.gamemode = buf.readByte();
    } else {
      this.gamemode = buf.readByte();
      this.isHardcore = (this.gamemode & 0x08) != 0;
      this.gamemode &= ~0x08;
    }
    this.previousGamemode = buf.readByte();

    this.levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));
    this.registry = ProtocolUtils.readCompoundTag(buf, version, JOINGAME_READER);
    String dimensionIdentifier;
    String levelName = null;
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)
        && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
      this.currentDimensionData = ProtocolUtils.readCompoundTag(buf, version, JOINGAME_READER);
      dimensionIdentifier = ProtocolUtils.readString(buf);
    } else {
      dimensionIdentifier = ProtocolUtils.readString(buf);
      levelName = ProtocolUtils.readString(buf);
    }

    this.partialHashedSeed = buf.readLong();
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      this.maxPlayers = ProtocolUtils.readVarInt(buf);
    } else {
      this.maxPlayers = buf.readUnsignedByte();
    }

    this.viewDistance = ProtocolUtils.readVarInt(buf);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
      this.simulationDistance = ProtocolUtils.readVarInt(buf);
    }

    this.reducedDebugInfo = buf.readBoolean();
    this.showRespawnScreen = buf.readBoolean();

    boolean isDebug = buf.readBoolean();
    boolean isFlat = buf.readBoolean();
    this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug, version);

    // optional death location
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19) && buf.readBoolean()) {
      this.lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
      this.portalCooldown = ProtocolUtils.readVarInt(buf);
    }
  }

  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private void decode1202Up(ByteBuf buf, ProtocolVersion version) {
    this.entityId = buf.readInt();
    this.isHardcore = buf.readBoolean();

    this.levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));

    this.maxPlayers = ProtocolUtils.readVarInt(buf);

    this.viewDistance = ProtocolUtils.readVarInt(buf);
    this.simulationDistance = ProtocolUtils.readVarInt(buf);

    this.reducedDebugInfo = buf.readBoolean();
    this.showRespawnScreen = buf.readBoolean();
    this.doLimitedCrafting = buf.readBoolean();

    String dimensionKey = "";
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      dimension = ProtocolUtils.readVarInt(buf);
    } else {
      dimensionKey = ProtocolUtils.readString(buf);
    }
    String levelName = ProtocolUtils.readString(buf);
    this.partialHashedSeed = buf.readLong();

    this.gamemode = buf.readByte();
    this.previousGamemode = buf.readByte();

    boolean isDebug = buf.readBoolean();
    boolean isFlat = buf.readBoolean();
    this.dimensionInfo = new DimensionInfo(dimensionKey, levelName, isFlat, isDebug, version);

    // optional death location
    if (buf.readBoolean()) {
      this.lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
    }

    this.portalCooldown = ProtocolUtils.readVarInt(buf);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      this.seaLevel = ProtocolUtils.readVarInt(buf);
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      this.enforcesSecureChat = buf.readBoolean();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      // haha funny, they made 1.20.2 more complicated
      this.encode1202Up(buf, version);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
      // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
      // so separate it out.
      this.encode116Up(buf, version);
    } else {
      this.encodeLegacy(buf, version);
    }
  }

  private void encodeLegacy(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9_1)) {
      buf.writeInt(dimension);
    } else {
      buf.writeByte(dimension);
    }
    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
      buf.writeByte(difficulty);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
      buf.writeLong(partialHashedSeed);
    }
    buf.writeByte(maxPlayers);
    if (levelType == null) {
      throw new IllegalStateException("No level type specified.");
    }
    ProtocolUtils.writeString(buf, levelType);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
      ProtocolUtils.writeVarInt(buf, viewDistance);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeBoolean(reducedDebugInfo);
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
      buf.writeBoolean(showRespawnScreen);
    }
  }

  private void encode116Up(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      buf.writeBoolean(isHardcore);
      buf.writeByte(gamemode);
    } else {
      buf.writeByte(isHardcore ? gamemode | 0x8 : gamemode);
    }
    buf.writeByte(previousGamemode);

    ProtocolUtils.writeStringArray(buf, levelNames.toArray(String[]::new));
    ProtocolUtils.writeBinaryTag(buf, version, this.registry);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2) && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
      ProtocolUtils.writeBinaryTag(buf, version, currentDimensionData);
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
    } else {
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
    }

    buf.writeLong(partialHashedSeed);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
      ProtocolUtils.writeVarInt(buf, maxPlayers);
    } else {
      buf.writeByte(maxPlayers);
    }

    ProtocolUtils.writeVarInt(buf, viewDistance);
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
      ProtocolUtils.writeVarInt(buf, simulationDistance);
    }

    buf.writeBoolean(reducedDebugInfo);
    buf.writeBoolean(showRespawnScreen);

    buf.writeBoolean(dimensionInfo.isDebugType());
    buf.writeBoolean(dimensionInfo.isFlat());

    // optional death location
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      if (lastDeathPosition != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeString(buf, lastDeathPosition.key());
        buf.writeLong(lastDeathPosition.value());
      } else {
        buf.writeBoolean(false);
      }
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
      ProtocolUtils.writeVarInt(buf, portalCooldown);
    }
  }

  private void encode1202Up(ByteBuf buf, ProtocolVersion version) {
    buf.writeInt(entityId);
    buf.writeBoolean(isHardcore);

    ProtocolUtils.writeStringArray(buf, levelNames.toArray(String[]::new));

    ProtocolUtils.writeVarInt(buf, maxPlayers);

    ProtocolUtils.writeVarInt(buf, viewDistance);
    ProtocolUtils.writeVarInt(buf, simulationDistance);

    buf.writeBoolean(reducedDebugInfo);
    buf.writeBoolean(showRespawnScreen);
    buf.writeBoolean(doLimitedCrafting);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      ProtocolUtils.writeVarInt(buf, dimension);
    } else {
      ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
    }
    ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
    buf.writeLong(partialHashedSeed);

    buf.writeByte(gamemode);
    buf.writeByte(previousGamemode);

    buf.writeBoolean(dimensionInfo.isDebugType());
    buf.writeBoolean(dimensionInfo.isFlat());

    // optional death location
    if (lastDeathPosition != null) {
      buf.writeBoolean(true);
      ProtocolUtils.writeString(buf, lastDeathPosition.key());
      buf.writeLong(lastDeathPosition.value());
    } else {
      buf.writeBoolean(false);
    }

    ProtocolUtils.writeVarInt(buf, portalCooldown);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
      ProtocolUtils.writeVarInt(buf, seaLevel);
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      buf.writeBoolean(this.enforcesSecureChat);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
