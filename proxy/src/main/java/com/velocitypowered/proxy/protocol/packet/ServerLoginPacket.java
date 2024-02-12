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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class ServerLoginPacket implements MinecraftPacket {

  private static final QuietDecoderException EMPTY_USERNAME = new QuietDecoderException(
      "Empty username!");

  private @Nullable String username;
  private @Nullable IdentifiedKey playerKey; // Introduced in 1.19.3
  private @Nullable UUID holderUuid; // Used for key revision 2

  public ServerLoginPacket() {
  }

  public ServerLoginPacket(String username, @Nullable IdentifiedKey playerKey) {
    this.username = Preconditions.checkNotNull(username, "username");
    this.playerKey = playerKey;
  }

  public ServerLoginPacket(String username, @Nullable UUID holderUuid) {
    this.username = Preconditions.checkNotNull(username, "username");
    this.holderUuid = holderUuid;
    this.playerKey = null;
  }

  public String getUsername() {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    return username;
  }

  public @Nullable IdentifiedKey getPlayerKey() {
    return this.playerKey;
  }

  public void setPlayerKey(IdentifiedKey playerKey) {
    this.playerKey = playerKey;
  }

  public @Nullable UUID getHolderUuid() {
    return holderUuid;
  }

  @Override
  public String toString() {
    return "ServerLogin{"
        + "username='" + username + '\''
        + "uuid='" + holderUuid + '\''
        + "playerKey='" + playerKey + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion version) {
    username = ProtocolUtils.readString(buf, 16);
    if (username.isEmpty()) {
      throw EMPTY_USERNAME;
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        playerKey = null;
      } else {
        if (buf.readBoolean()) {
          playerKey = ProtocolUtils.readPlayerKey(version, buf);
        } else {
          playerKey = null;
        }
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        this.holderUuid = ProtocolUtils.readUuid(buf);
        return;
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        if (buf.readBoolean()) {
          holderUuid = ProtocolUtils.readUuid(buf);
        }
      }
    } else {
      playerKey = null;
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    ProtocolUtils.writeString(buf, username);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        if (playerKey != null) {
          buf.writeBoolean(true);
          ProtocolUtils.writePlayerKey(buf, playerKey);
        } else {
          buf.writeBoolean(false);
        }
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
        ProtocolUtils.writeUuid(buf, this.holderUuid);
        return;
      }

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        if (playerKey != null && playerKey.getSignatureHolder() != null) {
          buf.writeBoolean(true);
          ProtocolUtils.writeUuid(buf, playerKey.getSignatureHolder());
        } else if (this.holderUuid != null) {
          buf.writeBoolean(true);
          ProtocolUtils.writeUuid(buf, this.holderUuid);
        } else {
          buf.writeBoolean(false);
        }
      }
    }
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    // Accommodate the rare (but likely malicious) use of UTF-8 usernames, since it is technically
    // legal on the protocol level.
    int base = 1 + (16 * 3);
    // Adjustments for Key-authentication
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
      if (version.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        // + 1 for the boolean present/ not present
        // + 8 for the long expiry
        // + 2 len for varint key size
        // + 294 for the key
        // + 2 len for varint signature size
        // + 512 for signature
        base += 1 + 8 + 2 + 294 + 2 + 512;
      }
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        // +1 boolean uuid optional
        // + 2 * 8 for the long msb/lsb
        base += 1 + 8 + 8;
      }
    }
    return base;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
