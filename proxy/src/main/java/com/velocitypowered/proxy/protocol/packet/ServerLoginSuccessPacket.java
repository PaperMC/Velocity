/*
 * Copyright (C) 2018-2022 Velocity Contributors
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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.VelocityProperties;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;

public final class ServerLoginSuccessPacket implements MinecraftPacket {

  private final UUID uuid;
  private final String username;
  private final List<GameProfile.Property> properties;
  private static final boolean strictErrorHandling = VelocityProperties
          .readBoolean("velocity.strictErrorHandling", true);

  public ServerLoginSuccessPacket(UUID uuid, String username, List<GameProfile.Property> properties) {
    this.uuid = uuid;
    this.username = username;
    this.properties = properties;
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getUsername() {
    return username;
  }

  public List<GameProfile.Property> getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return "ServerLoginSuccess{"
        + "uuid=" + uuid
        + ", username='" + username + '\''
        + ", properties='" + properties + '\''
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<ServerLoginSuccessPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ServerLoginSuccessPacket decode(ByteBuf buf, Direction direction,
        ProtocolVersion version) {
      UUID uuid;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        uuid = ProtocolUtils.readUuid(buf);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        uuid = ProtocolUtils.readUuidIntArray(buf);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        uuid = UUID.fromString(ProtocolUtils.readString(buf, 36));
      } else {
        uuid = UuidUtils.fromUndashed(ProtocolUtils.readString(buf, 32));
      }
      String username = ProtocolUtils.readString(buf, 16);

      List<GameProfile.Property> properties = null;
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        properties = ProtocolUtils.readProperties(buf);
      }
      if (version == ProtocolVersion.MINECRAFT_1_20_5 || version == ProtocolVersion.MINECRAFT_1_21) {
        buf.readBoolean();
      }

      return new ServerLoginSuccessPacket(uuid, username, properties);
    }

    @Override
    public void encode(ServerLoginSuccessPacket packet, ByteBuf buf, Direction direction,
        ProtocolVersion version) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        ProtocolUtils.writeUuid(buf, packet.uuid);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
        ProtocolUtils.writeUuidIntArray(buf, packet.uuid);
      } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        ProtocolUtils.writeString(buf, packet.uuid.toString());
      } else {
        ProtocolUtils.writeString(buf, UuidUtils.toUndashed(packet.uuid));
      }
      ProtocolUtils.writeString(buf, packet.username);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
        if (packet.properties == null) {
          ProtocolUtils.writeVarInt(buf, 0);
        } else {
          ProtocolUtils.writeProperties(buf, packet.properties);
        }
      }
      if (version == ProtocolVersion.MINECRAFT_1_20_5 || version == ProtocolVersion.MINECRAFT_1_21) {
        buf.writeBoolean(strictErrorHandling);
      }
    }

    @Override
    public int encodeSizeHint(ServerLoginSuccessPacket packet, Direction direction, ProtocolVersion version) {
      // We could compute an exact size, but 4KiB ought to be enough to encode all reasonable
      // sizes of this packet.
      return 4 * 1024;
    }

  }
}
