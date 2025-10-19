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
import com.velocitypowered.proxy.protocol.PacketCodec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class JoinGamePacket implements MinecraftPacket {

  private static final BinaryTagIO.Reader JOINGAME_READER = BinaryTagIO.reader(4 * 1024 * 1024);

  private final int entityId;
  private final short gamemode;
  private final int dimension;
  private final long partialHashedSeed;
  private final short difficulty;
  private final boolean isHardcore;
  private final int maxPlayers;
  private final @Nullable String levelType;
  private final int viewDistance;
  private final boolean reducedDebugInfo;
  private final boolean showRespawnScreen;
  private final boolean doLimitedCrafting;
  private final ImmutableSet<String> levelNames;
  private final CompoundBinaryTag registry;
  private final DimensionInfo dimensionInfo;
  private final CompoundBinaryTag currentDimensionData;
  private final short previousGamemode;
  private final int simulationDistance;
  private final @Nullable Pair<String, Long> lastDeathPosition;
  private final int portalCooldown;
  private final int seaLevel;
  private final boolean enforcesSecureChat;

  public JoinGamePacket(int entityId, short gamemode, int dimension, long partialHashedSeed,
      short difficulty, boolean isHardcore, int maxPlayers, @Nullable String levelType,
      int viewDistance, boolean reducedDebugInfo, boolean showRespawnScreen,
      boolean doLimitedCrafting, ImmutableSet<String> levelNames, CompoundBinaryTag registry,
      DimensionInfo dimensionInfo, CompoundBinaryTag currentDimensionData, short previousGamemode,
      int simulationDistance, @Nullable Pair<String, Long> lastDeathPosition, int portalCooldown,
      int seaLevel, boolean enforcesSecureChat) {
    this.entityId = entityId;
    this.gamemode = gamemode;
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.difficulty = difficulty;
    this.isHardcore = isHardcore;
    this.maxPlayers = maxPlayers;
    this.levelType = levelType;
    this.viewDistance = viewDistance;
    this.reducedDebugInfo = reducedDebugInfo;
    this.showRespawnScreen = showRespawnScreen;
    this.doLimitedCrafting = doLimitedCrafting;
    this.levelNames = levelNames;
    this.registry = registry;
    this.dimensionInfo = dimensionInfo;
    this.currentDimensionData = currentDimensionData;
    this.previousGamemode = previousGamemode;
    this.simulationDistance = simulationDistance;
    this.lastDeathPosition = lastDeathPosition;
    this.portalCooldown = portalCooldown;
    this.seaLevel = seaLevel;
    this.enforcesSecureChat = enforcesSecureChat;
  }

  public int getEntityId() {
    return entityId;
  }

  public short getGamemode() {
    return gamemode;
  }

  public int getDimension() {
    return dimension;
  }

  public long getPartialHashedSeed() {
    return partialHashedSeed;
  }

  public short getDifficulty() {
    return difficulty;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public @Nullable String getLevelType() {
    return levelType;
  }

  public int getViewDistance() {
    return viewDistance;
  }

  public boolean isReducedDebugInfo() {
    return reducedDebugInfo;
  }

  public DimensionInfo getDimensionInfo() {
    return dimensionInfo;
  }

  public short getPreviousGamemode() {
    return previousGamemode;
  }

  public boolean getIsHardcore() {
    return isHardcore;
  }

  public boolean getDoLimitedCrafting() {
    return doLimitedCrafting;
  }

  public CompoundBinaryTag getCurrentDimensionData() {
    return currentDimensionData;
  }

  public int getSimulationDistance() {
    return simulationDistance;
  }

  public Pair<String, Long> getLastDeathPosition() {
    return lastDeathPosition;
  }

  public int getPortalCooldown() {
    return portalCooldown;
  }

  public int getSeaLevel() {
    return seaLevel;
  }

  public boolean getEnforcesSecureChat() {
    return this.enforcesSecureChat;
  }

  public CompoundBinaryTag getRegistry() {
    return registry;
  }

  public boolean getShowRespawnScreen() {
    return showRespawnScreen;
  }

  public ImmutableSet<String> getLevelNames() {
    return levelNames;
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
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<JoinGamePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public JoinGamePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        // haha funny, they made 1.20.2 more complicated
        return decode1202Up(buf, version);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
        // so separate it out.
        return decode116Up(buf, version);
      } else {
        return decodeLegacy(buf, version);
      }
    }

    @Override
    public void encode(JoinGamePacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        // haha funny, they made 1.20.2 more complicated
        encode1202Up(packet, buf, version);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        // Minecraft 1.16 and above have significantly more complicated logic for reading this packet,
        // so separate it out.
        encode116Up(packet, buf, version);
      } else {
        encodeLegacy(packet, buf, version);
      }
    }

    private static JoinGamePacket decodeLegacy(ByteBuf buf, ProtocolVersion version) {
      int entityId = buf.readInt();
      short gamemode = buf.readByte();
      boolean isHardcore = (gamemode & 0x08) != 0;
      gamemode &= ~0x08;

      int dimension;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9_1)) {
        dimension = buf.readInt();
      } else {
        dimension = buf.readByte();
      }

      short difficulty = 0;
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        difficulty = buf.readUnsignedByte();
      }

      long partialHashedSeed = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        partialHashedSeed = buf.readLong();
      }

      int maxPlayers = buf.readUnsignedByte();
      String levelType = ProtocolUtils.readString(buf, 16);

      int viewDistance = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
        viewDistance = ProtocolUtils.readVarInt(buf);
      }

      boolean reducedDebugInfo = false;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        reducedDebugInfo = buf.readBoolean();
      }

      boolean showRespawnScreen = true;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        showRespawnScreen = buf.readBoolean();
      }

      return new JoinGamePacket(entityId, gamemode, dimension, partialHashedSeed, difficulty,
          isHardcore, maxPlayers, levelType, viewDistance, reducedDebugInfo, showRespawnScreen,
          false, ImmutableSet.of(), null, null, null, (short) 0, 0, null, 0, 0, false);
    }

    private static JoinGamePacket decode116Up(ByteBuf buf, ProtocolVersion version) {
      int entityId = buf.readInt();
      boolean isHardcore;
      short gamemode;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        isHardcore = buf.readBoolean();
        gamemode = buf.readByte();
      } else {
        gamemode = buf.readByte();
        isHardcore = (gamemode & 0x08) != 0;
        gamemode &= ~0x08;
      }
      short previousGamemode = buf.readByte();

      ImmutableSet<String> levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));
      CompoundBinaryTag registry = ProtocolUtils.readCompoundTag(buf, version, JOINGAME_READER);

      String dimensionIdentifier;
      String levelName = null;
      CompoundBinaryTag currentDimensionData = null;

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)
          && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
        currentDimensionData = ProtocolUtils.readCompoundTag(buf, version, JOINGAME_READER);
        dimensionIdentifier = ProtocolUtils.readString(buf);
      } else {
        dimensionIdentifier = ProtocolUtils.readString(buf);
        levelName = ProtocolUtils.readString(buf);
      }

      long partialHashedSeed = buf.readLong();

      int maxPlayers;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        maxPlayers = ProtocolUtils.readVarInt(buf);
      } else {
        maxPlayers = buf.readUnsignedByte();
      }

      int viewDistance = ProtocolUtils.readVarInt(buf);

      int simulationDistance = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
        simulationDistance = ProtocolUtils.readVarInt(buf);
      }

      boolean reducedDebugInfo = buf.readBoolean();
      boolean showRespawnScreen = buf.readBoolean();

      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      DimensionInfo dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug, version);

      Pair<String, Long> lastDeathPosition = null;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19) && buf.readBoolean()) {
        lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
      }

      int portalCooldown = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        portalCooldown = ProtocolUtils.readVarInt(buf);
      }

      return new JoinGamePacket(entityId, gamemode, 0, partialHashedSeed, (short) 0, isHardcore,
          maxPlayers, null, viewDistance, reducedDebugInfo, showRespawnScreen, false, levelNames,
          registry, dimensionInfo, currentDimensionData, previousGamemode, simulationDistance,
          lastDeathPosition, portalCooldown, 0, false);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private static JoinGamePacket decode1202Up(ByteBuf buf, ProtocolVersion version) {
      int entityId = buf.readInt();
      boolean isHardcore = buf.readBoolean();

      ImmutableSet<String> levelNames = ImmutableSet.copyOf(ProtocolUtils.readStringArray(buf));

      int maxPlayers = ProtocolUtils.readVarInt(buf);

      int viewDistance = ProtocolUtils.readVarInt(buf);
      int simulationDistance = ProtocolUtils.readVarInt(buf);

      boolean reducedDebugInfo = buf.readBoolean();
      boolean showRespawnScreen = buf.readBoolean();
      boolean doLimitedCrafting = buf.readBoolean();

      int dimension = 0;
      String dimensionKey = "";
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        dimension = ProtocolUtils.readVarInt(buf);
      } else {
        dimensionKey = ProtocolUtils.readString(buf);
      }
      String levelName = ProtocolUtils.readString(buf);
      long partialHashedSeed = buf.readLong();

      short gamemode = buf.readByte();
      short previousGamemode = buf.readByte();

      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      DimensionInfo dimensionInfo = new DimensionInfo(dimensionKey, levelName, isFlat, isDebug, version);

      Pair<String, Long> lastDeathPosition = null;
      if (buf.readBoolean()) {
        lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
      }

      int portalCooldown = ProtocolUtils.readVarInt(buf);

      int seaLevel = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        seaLevel = ProtocolUtils.readVarInt(buf);
      }

      boolean enforcesSecureChat = false;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        enforcesSecureChat = buf.readBoolean();
      }

      return new JoinGamePacket(entityId, gamemode, dimension, partialHashedSeed, (short) 0,
          isHardcore, maxPlayers, null, viewDistance, reducedDebugInfo, showRespawnScreen,
          doLimitedCrafting, levelNames, null, dimensionInfo, null, previousGamemode,
          simulationDistance, lastDeathPosition, portalCooldown, seaLevel, enforcesSecureChat);
    }

    private static void encodeLegacy(JoinGamePacket packet, ByteBuf buf, ProtocolVersion version) {
      buf.writeInt(packet.entityId);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        buf.writeBoolean(packet.isHardcore);
        buf.writeByte(packet.gamemode);
      } else {
        buf.writeByte(packet.isHardcore ? packet.gamemode | 0x8 : packet.gamemode);
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9_1)) {
        buf.writeInt(packet.dimension);
      } else {
        buf.writeByte(packet.dimension);
      }
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        buf.writeByte(packet.difficulty);
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        buf.writeLong(packet.partialHashedSeed);
      }
      buf.writeByte(packet.maxPlayers);
      if (packet.levelType == null) {
        throw new IllegalStateException("No level type specified.");
      }
      ProtocolUtils.writeString(buf, packet.levelType);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_14)) {
        ProtocolUtils.writeVarInt(buf, packet.viewDistance);
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
        buf.writeBoolean(packet.reducedDebugInfo);
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        buf.writeBoolean(packet.showRespawnScreen);
      }
    }

    private static void encode116Up(JoinGamePacket packet, ByteBuf buf, ProtocolVersion version) {
      buf.writeInt(packet.entityId);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        buf.writeBoolean(packet.isHardcore);
        buf.writeByte(packet.gamemode);
      } else {
        buf.writeByte(packet.isHardcore ? packet.gamemode | 0x8 : packet.gamemode);
      }
      buf.writeByte(packet.previousGamemode);

      ProtocolUtils.writeStringArray(buf, packet.levelNames.toArray(String[]::new));
      ProtocolUtils.writeBinaryTag(buf, version, packet.registry);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2) && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
        ProtocolUtils.writeBinaryTag(buf, version, packet.currentDimensionData);
        ProtocolUtils.writeString(buf, packet.dimensionInfo.getRegistryIdentifier());
      } else {
        ProtocolUtils.writeString(buf, packet.dimensionInfo.getRegistryIdentifier());
        ProtocolUtils.writeString(buf, packet.dimensionInfo.getLevelName());
      }

      buf.writeLong(packet.partialHashedSeed);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)) {
        ProtocolUtils.writeVarInt(buf, packet.maxPlayers);
      } else {
        buf.writeByte(packet.maxPlayers);
      }

      ProtocolUtils.writeVarInt(buf, packet.viewDistance);
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
        ProtocolUtils.writeVarInt(buf, packet.simulationDistance);
      }

      buf.writeBoolean(packet.reducedDebugInfo);
      buf.writeBoolean(packet.showRespawnScreen);

      buf.writeBoolean(packet.dimensionInfo.isDebugType());
      buf.writeBoolean(packet.dimensionInfo.isFlat());

      // optional death location
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        if (packet.lastDeathPosition != null) {
          buf.writeBoolean(true);
          ProtocolUtils.writeString(buf, packet.lastDeathPosition.key());
          buf.writeLong(packet.lastDeathPosition.value());
        } else {
          buf.writeBoolean(false);
        }
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        ProtocolUtils.writeVarInt(buf, packet.portalCooldown);
      }
    }

    private static void encode1202Up(JoinGamePacket packet, ByteBuf buf, ProtocolVersion version) {
      buf.writeInt(packet.entityId);
      buf.writeBoolean(packet.isHardcore);

      ProtocolUtils.writeStringArray(buf, packet.levelNames.toArray(String[]::new));

      ProtocolUtils.writeVarInt(buf, packet.maxPlayers);

      ProtocolUtils.writeVarInt(buf, packet.viewDistance);
      ProtocolUtils.writeVarInt(buf, packet.simulationDistance);

      buf.writeBoolean(packet.reducedDebugInfo);
      buf.writeBoolean(packet.showRespawnScreen);
      buf.writeBoolean(packet.doLimitedCrafting);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        ProtocolUtils.writeVarInt(buf, packet.dimension);
      } else {
        ProtocolUtils.writeString(buf, packet.dimensionInfo.getRegistryIdentifier());
      }
      ProtocolUtils.writeString(buf, packet.dimensionInfo.getLevelName());
      buf.writeLong(packet.partialHashedSeed);

      buf.writeByte(packet.gamemode);
      buf.writeByte(packet.previousGamemode);

      buf.writeBoolean(packet.dimensionInfo.isDebugType());
      buf.writeBoolean(packet.dimensionInfo.isFlat());

      // optional death location
      if (packet.lastDeathPosition != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeString(buf, packet.lastDeathPosition.key());
        buf.writeLong(packet.lastDeathPosition.value());
      } else {
        buf.writeBoolean(false);
      }

      ProtocolUtils.writeVarInt(buf, packet.portalCooldown);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        ProtocolUtils.writeVarInt(buf, packet.seaLevel);
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        buf.writeBoolean(packet.enforcesSecureChat);
      }
    }
  }
}
