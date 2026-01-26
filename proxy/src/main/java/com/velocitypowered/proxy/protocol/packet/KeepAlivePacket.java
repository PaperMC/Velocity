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
import io.netty.buffer.ByteBuf;

/**
 * Represents a KeepAlive packet in Minecraft. This packet is used to ensure that the connection
 * between the client and the server shall still be active by sending a randomly generated ID that
 * the client must respond to.
 */
public class KeepAlivePacket implements MinecraftPacket {

  private long randomId;

  public long getRandomId() {
    return randomId;
  }

  public void setRandomId(long randomId) {
    this.randomId = randomId;
  }

  @Override
  public String toString() {
    return "KeepAlive{"
        + "randomId=" + randomId
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_12_2)) {
      randomId = buf.readLong();
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      randomId = ProtocolUtils.readVarInt(buf);
    } else {
      randomId = buf.readInt();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_12_2)) {
      buf.writeLong(randomId);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeVarInt(buf, (int) randomId);
    } else {
      buf.writeInt((int) randomId);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
