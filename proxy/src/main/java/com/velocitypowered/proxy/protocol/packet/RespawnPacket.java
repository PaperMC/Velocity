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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DimensionInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class RespawnPacket implements MinecraftPacket {

  private final int dimension;
  private final long partialHashedSeed;
  private final short difficulty;
  private final short gamemode;
  private final String levelType;
  private final byte dataToKeep;
  private final DimensionInfo dimensionInfo;
  private final short previousGamemode;
  private final CompoundBinaryTag currentDimensionData;
  private final @Nullable Pair<String, Long> lastDeathPosition;
  private final int portalCooldown;
  private final int seaLevel;

  public RespawnPacket(int dimension, long partialHashedSeed, short difficulty, short gamemode,
                       String levelType, byte dataToKeep, DimensionInfo dimensionInfo,
                       short previousGamemode, CompoundBinaryTag currentDimensionData,
                       @Nullable Pair<String, Long> lastDeathPosition, int portalCooldown,
                       int seaLevel) {
    this.dimension = dimension;
    this.partialHashedSeed = partialHashedSeed;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.levelType = levelType;
    this.dataToKeep = dataToKeep;
    this.dimensionInfo = dimensionInfo;
    this.previousGamemode = previousGamemode;
    this.currentDimensionData = currentDimensionData;
    this.lastDeathPosition = lastDeathPosition;
    this.portalCooldown = portalCooldown;
    this.seaLevel = seaLevel;
  }

  public static RespawnPacket fromJoinGame(JoinGamePacket joinGame) {
    return new RespawnPacket(joinGame.getDimension(), joinGame.getPartialHashedSeed(),
        joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
        (byte) 0, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
        joinGame.getCurrentDimensionData(), joinGame.getLastDeathPosition(),
        joinGame.getPortalCooldown(), joinGame.getSeaLevel());
  }

  public static RespawnPacket fromJoinGame(JoinGamePacket joinGame, int newDimension) {
    return new RespawnPacket(newDimension, joinGame.getPartialHashedSeed(),
        joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
        (byte) 0, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
        joinGame.getCurrentDimensionData(), joinGame.getLastDeathPosition(),
        joinGame.getPortalCooldown(), joinGame.getSeaLevel());
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

  public short getGamemode() {
    return gamemode;
  }

  public String getLevelType() {
    return levelType;
  }

  public byte getDataToKeep() {
    return dataToKeep;
  }

  public short getPreviousGamemode() {
    return previousGamemode;
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

  public DimensionInfo getDimensionInfo() {
    return dimensionInfo;
  }

  public CompoundBinaryTag getCurrentDimensionData() {
    return currentDimensionData;
  }

  @Override
  public String toString() {
    return "Respawn{"
        + "dimension=" + dimension
        + ", partialHashedSeed=" + partialHashedSeed
        + ", difficulty=" + difficulty
        + ", gamemode=" + gamemode
        + ", levelType='" + levelType + '\''
        + ", dataToKeep=" + dataToKeep
        + ", dimensionRegistryName='" + dimensionInfo.toString() + '\''
        + ", dimensionInfo=" + dimensionInfo
        + ", previousGamemode=" + previousGamemode
        + ", dimensionData=" + currentDimensionData
        + ", portalCooldown=" + portalCooldown
        + ", seaLevel=" + seaLevel
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<RespawnPacket> {
    @Override
    public RespawnPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      String dimensionKey = "";
      String levelName = null;
      int dimension = 0;
      CompoundBinaryTag currentDimensionData = null;

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)
            && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
          currentDimensionData = ProtocolUtils.readCompoundTag(buf, version, BinaryTagIO.reader());
          dimensionKey = ProtocolUtils.readString(buf);
        } else {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
            dimension = ProtocolUtils.readVarInt(buf);
          } else {
            dimensionKey = ProtocolUtils.readString(buf);
          }
          levelName = ProtocolUtils.readString(buf);
        }
      } else {
        dimension = buf.readInt();
      }

      short difficulty = 0;
      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        difficulty = buf.readUnsignedByte();
      }

      long partialHashedSeed = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        partialHashedSeed = buf.readLong();
      }

      short gamemode = buf.readByte();

      DimensionInfo dimensionInfo = null;
      String levelType = "";
      short previousGamemode = 0;
      byte dataToKeep = 0;

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        previousGamemode = buf.readByte();
        boolean isDebug = buf.readBoolean();
        boolean isFlat = buf.readBoolean();
        dimensionInfo = new DimensionInfo(dimensionKey, levelName, isFlat, isDebug, version);
        if (version.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
          dataToKeep = (byte) (buf.readBoolean() ? 1 : 0);
        } else if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
          dataToKeep = buf.readByte();
        }
      } else {
        levelType = ProtocolUtils.readString(buf, 16);
      }

      Pair<String, Long> lastDeathPosition = null;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19) && buf.readBoolean()) {
        lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
      }

      int portalCooldown = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20)) {
        portalCooldown = ProtocolUtils.readVarInt(buf);
      }

      int seaLevel = 0;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        seaLevel = ProtocolUtils.readVarInt(buf);
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        dataToKeep = buf.readByte();
      }

      return new RespawnPacket(dimension, partialHashedSeed, difficulty, gamemode, levelType,
          dataToKeep, dimensionInfo, previousGamemode, currentDimensionData, lastDeathPosition,
          portalCooldown, seaLevel);
    }

    @Override
    public void encode(RespawnPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16_2)
            && version.lessThan(ProtocolVersion.MINECRAFT_1_19)) {
          ProtocolUtils.writeBinaryTag(buf, version, packet.currentDimensionData);
          ProtocolUtils.writeString(buf, packet.dimensionInfo.getRegistryIdentifier());
        } else {
          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
            ProtocolUtils.writeVarInt(buf, packet.dimension);
          } else {
            ProtocolUtils.writeString(buf, packet.dimensionInfo.getRegistryIdentifier());
          }
          ProtocolUtils.writeString(buf, packet.dimensionInfo.getLevelName());
        }
      } else {
        buf.writeInt(packet.dimension);
      }

      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_13_2)) {
        buf.writeByte(packet.difficulty);
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_15)) {
        buf.writeLong(packet.partialHashedSeed);
      }

      buf.writeByte(packet.gamemode);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        buf.writeByte(packet.previousGamemode);
        buf.writeBoolean(packet.dimensionInfo.isDebugType());
        buf.writeBoolean(packet.dimensionInfo.isFlat());
        if (version.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
          buf.writeBoolean(packet.dataToKeep != 0);
        } else if (version.lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
          buf.writeByte(packet.dataToKeep);
        }
      } else {
        ProtocolUtils.writeString(buf, packet.levelType);
      }

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

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
        ProtocolUtils.writeVarInt(buf, packet.seaLevel);
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        buf.writeByte(packet.dataToKeep);
      }
    }
  }
}
