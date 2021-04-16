/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundBossBarPacket implements Packet {

  public static final PacketReader<ClientboundBossBarPacket> DECODER = PacketReader.method(ClientboundBossBarPacket::new);
  public static final PacketWriter<ClientboundBossBarPacket> ENCODER = PacketWriter.deprecatedEncode();

  public static final int ADD = 0;
  public static final int REMOVE = 1;
  public static final int UPDATE_PERCENT = 2;
  public static final int UPDATE_NAME = 3;
  public static final int UPDATE_STYLE = 4;
  public static final int UPDATE_PROPERTIES = 5;

  private @Nullable UUID uuid;
  private int action;
  private @Nullable String name;
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

  public @Nullable String getName() {
    return name;
  }

  public void setName(String name) {
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
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    this.uuid = ProtocolUtils.readUuid(buf);
    this.action = ProtocolUtils.readVarInt(buf);
    switch (action) {
      case ADD:
        this.name = ProtocolUtils.readString(buf);
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
        this.name = ProtocolUtils.readString(buf);
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
  public void encode(ByteBuf buf, ProtocolVersion version) {
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
        ProtocolUtils.writeString(buf, name);
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
        ProtocolUtils.writeString(buf, name);
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
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public static ClientboundBossBarPacket createRemovePacket(UUID id) {
    ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
    packet.setUuid(id);
    packet.setAction(REMOVE);
    return packet;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("uuid", this.uuid)
      .add("action", this.action)
      .add("name", this.name)
      .add("percent", this.percent)
      .add("color", this.color)
      .add("overlay", this.overlay)
      .add("flags", this.flags)
      .toString();
  }
}
