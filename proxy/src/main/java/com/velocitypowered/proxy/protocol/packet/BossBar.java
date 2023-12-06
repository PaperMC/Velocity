/*
 * Copyright (C) 2018-2021 Velocity Contributors
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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BossBar implements MinecraftPacket {

  public static final int ADD = 0;
  public static final int REMOVE = 1;
  public static final int UPDATE_PERCENT = 2;
  public static final int UPDATE_NAME = 3;
  public static final int UPDATE_STYLE = 4;
  public static final int UPDATE_PROPERTIES = 5;
  private @Nullable UUID uuid;
  private int action;
  private @Nullable ComponentHolder name;
  private float percent;
  private int color;
  private int overlay;
  private short flags;

  public UUID getUuid() {
    if (uuid == null) {
      throw new IllegalStateException("No boss bar UUID specified");
    }
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public int getAction() {
    return action;
  }

  public void setAction(int action) {
    this.action = action;
  }

  public @Nullable ComponentHolder getName() {
    return name;
  }

  public void setName(ComponentHolder name) {
    this.name = name;
  }

  public float getPercent() {
    return percent;
  }

  public void setPercent(float percent) {
    this.percent = percent;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public int getOverlay() {
    return overlay;
  }

  public void setOverlay(int overlay) {
    this.overlay = overlay;
  }

  public short getFlags() {
    return flags;
  }

  public void setFlags(short flags) {
    this.flags = flags;
  }

  @Override
  public String toString() {
    return "BossBar{"
        + "uuid=" + uuid
        + ", action=" + action
        + ", name='" + name + '\''
        + ", percent=" + percent
        + ", color=" + color
        + ", overlay=" + overlay
        + ", flags=" + flags
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.uuid = ProtocolUtils.readUuid(buf);
    this.action = ProtocolUtils.readVarInt(buf);
    switch (action) {
      case ADD:
        this.name = ComponentHolder.read(buf, version);
        this.percent = buf.readFloat();
        this.color = ProtocolUtils.readVarInt(buf);
        this.overlay = ProtocolUtils.readVarInt(buf);
        this.flags = buf.readUnsignedByte();
        break;
      case REMOVE:
        break;
      case UPDATE_PERCENT:
        this.percent = buf.readFloat();
        break;
      case UPDATE_NAME:
        this.name = ComponentHolder.read(buf, version);
        break;
      case UPDATE_STYLE:
        this.color = ProtocolUtils.readVarInt(buf);
        this.overlay = ProtocolUtils.readVarInt(buf);
        break;
      case UPDATE_PROPERTIES:
        this.flags = buf.readUnsignedByte();
        break;
      default:
        throw new UnsupportedOperationException("Unknown action " + action);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (uuid == null) {
      throw new IllegalStateException("No boss bar UUID specified");
    }
    ProtocolUtils.writeUuid(buf, uuid);
    ProtocolUtils.writeVarInt(buf, action);
    switch (action) {
      case ADD:
        if (name == null) {
          throw new IllegalStateException("No name specified!");
        }
        name.write(buf);
        buf.writeFloat(percent);
        ProtocolUtils.writeVarInt(buf, color);
        ProtocolUtils.writeVarInt(buf, overlay);
        buf.writeByte(flags);
        break;
      case REMOVE:
        break;
      case UPDATE_PERCENT:
        buf.writeFloat(percent);
        break;
      case UPDATE_NAME:
        if (name == null) {
          throw new IllegalStateException("No name specified!");
        }
        name.write(buf);
        break;
      case UPDATE_STYLE:
        ProtocolUtils.writeVarInt(buf, color);
        ProtocolUtils.writeVarInt(buf, overlay);
        break;
      case UPDATE_PROPERTIES:
        buf.writeByte(flags);
        break;
      default:
        throw new UnsupportedOperationException("Unknown action " + action);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static BossBar createRemovePacket(UUID id) {
    BossBar packet = new BossBar();
    packet.setUuid(id);
    packet.setAction(REMOVE);
    return packet;
  }
}