package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Respawn implements MinecraftPacket {

  private int dimension;
  private short difficulty;
  private short gamemode;
  private String levelType = "";

  public Respawn() {
  }

  public Respawn(int dimension, short difficulty, short gamemode, String levelType) {
    this.dimension = dimension;
    this.difficulty = difficulty;
    this.gamemode = gamemode;
    this.levelType = levelType;
  }

  public int getDimension() {
    return dimension;
  }

  public void setDimension(int dimension) {
    this.dimension = dimension;
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

  @Override
  public String toString() {
    return "Respawn{"
        + "dimension=" + dimension
        + ", difficulty=" + difficulty
        + ", gamemode=" + gamemode
        + ", levelType='" + levelType + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.dimension = buf.readInt();
    this.difficulty = buf.readUnsignedByte();
    this.gamemode = buf.readUnsignedByte();
    this.levelType = ProtocolUtils.readString(buf, 16);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(dimension);
    buf.writeByte(difficulty);
    buf.writeByte(gamemode);
    ProtocolUtils.writeString(buf, levelType);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
