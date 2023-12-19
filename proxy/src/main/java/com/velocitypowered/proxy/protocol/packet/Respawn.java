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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Respawn implements MinecraftPacket {

  private int dimension;
  private long partialHashedSeed;
  private short difficulty;
  private short gamemode;
  private String levelType = "";
  private byte dataToKeep; // 1.16+
  private DimensionInfo dimensionInfo; // 1.16-1.16.1
  private short previousGamemode; // 1.16+
  private CompoundBinaryTag currentDimensionData; // 1.16.2+
  private @Nullable Pair<String, Long> lastDeathPosition; // 1.19+
  private int portalCooldown; // 1.20+

  public Respawn() {
  }

  public Respawn(int dimension, long partialHashedSeed, short difficulty, short gamemode,
      String levelType, byte dataToKeep, DimensionInfo dimensionInfo,
      short previousGamemode, CompoundBinaryTag currentDimensionData,
      @Nullable Pair<String, Long> lastDeathPosition, int portalCooldown) {
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
  }

  public static Respawn fromJoinGame(JoinGame joinGame) {
    return new Respawn(joinGame.getDimension(), joinGame.getPartialHashedSeed(),
        joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
        (byte) 0, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
        joinGame.getCurrentDimensionData(), joinGame.getLastDeathPosition(), joinGame.getPortalCooldown());
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

  public byte getDataToKeep() {
    return dataToKeep;
  }

  public void setDataToKeep(byte dataToKeep) {
    this.dataToKeep = dataToKeep;
  }

  public short getPreviousGamemode() {
    return previousGamemode;
  }

  public void setPreviousGamemode(short previousGamemode) {
    this.previousGamemode = previousGamemode;
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
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    String dimensionIdentifier = null;
    String levelName = null;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0
          && version.compareTo(ProtocolVersion.MINECRAFT_1_19) < 0) {
        this.currentDimensionData = ProtocolUtils.readCompoundTag(buf, version, BinaryTagIO.reader());
        dimensionIdentifier = ProtocolUtils.readString(buf);
      } else {
        dimensionIdentifier = ProtocolUtils.readString(buf);
        levelName = ProtocolUtils.readString(buf);
      }
    } else {
      this.dimension = buf.readInt();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13_2) <= 0) {
      this.difficulty = buf.readUnsignedByte();
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_15) >= 0) {
      this.partialHashedSeed = buf.readLong();
    }
    this.gamemode = buf.readByte();
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      this.previousGamemode = buf.readByte();
      boolean isDebug = buf.readBoolean();
      boolean isFlat = buf.readBoolean();
      this.dimensionInfo = new DimensionInfo(dimensionIdentifier, levelName, isFlat, isDebug);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
        this.dataToKeep = (byte) (buf.readBoolean() ? 1 : 0);
      } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0) {
        this.dataToKeep = buf.readByte();
      }
    } else {
      this.levelType = ProtocolUtils.readString(buf, 16);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0 && buf.readBoolean()) {
      this.lastDeathPosition = Pair.of(ProtocolUtils.readString(buf), buf.readLong());
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20) >= 0) {
      this.portalCooldown = ProtocolUtils.readVarInt(buf);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      this.dataToKeep = buf.readByte();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0
          && version.compareTo(ProtocolVersion.MINECRAFT_1_19) < 0) {
        ProtocolUtils.writeBinaryTag(buf, version, currentDimensionData);
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
      } else {
        ProtocolUtils.writeString(buf, dimensionInfo.getRegistryIdentifier());
        ProtocolUtils.writeString(buf, dimensionInfo.getLevelName());
      }
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
      buf.writeByte(previousGamemode);
      buf.writeBoolean(dimensionInfo.isDebugType());
      buf.writeBoolean(dimensionInfo.isFlat());
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
        buf.writeBoolean(dataToKeep != 0);
      } else if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0) {
        buf.writeByte(dataToKeep);
      }
    } else {
      ProtocolUtils.writeString(buf, levelType);
    }

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

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20) >= 0) {
      ProtocolUtils.writeVarInt(buf, portalCooldown);
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_20_2) >= 0) {
      buf.writeByte(dataToKeep);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
