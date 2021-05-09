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
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;

public class ClientboundServerLoginSuccessPacket implements Packet {
  public static final PacketReader<ClientboundServerLoginSuccessPacket> DECODER = (buf, version) -> {
    final UUID uuid;
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      uuid = ProtocolUtils.readUuidIntArray(buf);
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_7_6)) {
      uuid = UUID.fromString(ProtocolUtils.readString(buf, 36));
    } else {
      uuid = UuidUtils.fromUndashed(ProtocolUtils.readString(buf, 32));
    }
    final String username = ProtocolUtils.readString(buf, 16);
    return new ClientboundServerLoginSuccessPacket(uuid, username);
  };
  public static final PacketWriter<ClientboundServerLoginSuccessPacket> ENCODER = (out, packet, version) -> {
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtils.writeUuidIntArray(out, packet.uuid);
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_7_6)) {
      ProtocolUtils.writeString(out, packet.uuid.toString());
    } else {
      ProtocolUtils.writeString(out, UuidUtils.toUndashed(packet.uuid));
    }
    ProtocolUtils.writeString(out, packet.username);
  };

  private final UUID uuid;
  private final String username;

  public ClientboundServerLoginSuccessPacket(final UUID uuid, final String username) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.username = Objects.requireNonNull(username, "username");
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("uuid", this.uuid)
      .add("username", this.username)
      .toString();
  }
}
