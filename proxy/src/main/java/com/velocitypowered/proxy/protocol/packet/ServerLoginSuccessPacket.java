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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.VelocityProperties;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the packet sent from the server to the client to indicate successful login.
 * This packet contains the player's UUID, username, and properties associated with their profile.
 */
public class ServerLoginSuccessPacket implements MinecraftPacket {

  private @Nullable UUID uuid;
  private @Nullable String username;
  private @Nullable List<GameProfile.Property> properties;
  private static final boolean strictErrorHandling = VelocityProperties
          .readBoolean("velocity.strictErrorHandling", true);

  /**
   * Gets the player's UUID from the login success packet.
   *
   * @return the player's UUID
   * @throws IllegalStateException if the UUID is not specified
   */
  public UUID getUuid() {
    if (uuid == null) {
      throw new IllegalStateException("No UUID specified!");
    }
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets the player's username from the login success packet.
   *
   * @return the player's username
   * @throws IllegalStateException if the username is not specified
   */
  public String getUsername() {
    if (username == null) {
      throw new IllegalStateException("No username specified!");
    }
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<GameProfile.Property> getProperties() {
    return properties;
  }

  public void setProperties(List<GameProfile.Property> properties) {
    this.properties = properties;
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
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      uuid = ProtocolUtils.readUuid(buf);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
      uuid = ProtocolUtils.readUuidIntArray(buf);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      uuid = UUID.fromString(ProtocolUtils.readString(buf, 36));
    } else {
      uuid = UuidUtils.fromUndashed(ProtocolUtils.readString(buf, 32));
    }
    username = ProtocolUtils.readString(buf, 16);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      properties = ProtocolUtils.readProperties(buf);
    }
    if (version == ProtocolVersion.MINECRAFT_1_20_5 || version == ProtocolVersion.MINECRAFT_1_21) {
      buf.readBoolean();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (uuid == null) {
      throw new IllegalStateException("No UUID specified!");
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      ProtocolUtils.writeUuid(buf, uuid);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtils.writeUuidIntArray(buf, uuid);
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      ProtocolUtils.writeString(buf, uuid.toString());
    } else {
      ProtocolUtils.writeString(buf, UuidUtils.toUndashed(uuid));
    }
    if (username == null) {
      throw new IllegalStateException("No username specified!");
    }
    ProtocolUtils.writeString(buf, username);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      if (properties == null) {
        ProtocolUtils.writeVarInt(buf, 0);
      } else {
        ProtocolUtils.writeProperties(buf, properties);
      }
    }
    if (version == ProtocolVersion.MINECRAFT_1_20_5 || version == ProtocolVersion.MINECRAFT_1_21) {
      buf.writeBoolean(strictErrorHandling);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public int encodeSizeHint(Direction direction, ProtocolVersion version) {
    // We could compute an exact size, but 4KiB ought to be enough to encode all reasonable
    // sizes of this packet.
    return 4 * 1024;
  }
}
