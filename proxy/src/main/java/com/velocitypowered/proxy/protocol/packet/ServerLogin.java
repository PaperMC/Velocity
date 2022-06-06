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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerLogin implements MinecraftPacket {

  private static final QuietDecoderException EMPTY_USERNAME = new QuietDecoderException("Empty username!");

  private @Nullable String username;
  private @Nullable IdentifiedKey playerKey; // Introduced in 1.19

  public ServerLogin() {
  }

  public ServerLogin(String username) {
    this.username = Preconditions.checkNotNull(username, "username");
  }

  public String getUsername() {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    return username;
  }

  public IdentifiedKey getPlayerKey() {
    return playerKey;
  }

  public void setPlayerKey(IdentifiedKey playerKey) {
    this.playerKey = playerKey;
  }

  @Override
  public String toString() {
    return "ServerLogin{"
        + "username='" + username + '\''
        + "playerKey='" + playerKey + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    username = ProtocolUtils.readString(buf, 16);
    if (username.isEmpty()) {
      throw EMPTY_USERNAME;
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (buf.readBoolean()) {
        long expiry = buf.readLong();
        byte[] key = ProtocolUtils.readByteArray(buf);
        byte[] signature = ProtocolUtils.readByteArray(buf, 4096);
        playerKey = new IdentifiedKeyImpl(key, expiry, signature);
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    ProtocolUtils.writeString(buf, username);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (playerKey != null) {
        buf.writeBoolean(true);
        buf.writeLong(playerKey.getExpiryTemporal().toEpochMilli());
        ProtocolUtils.writeByteArray(buf, playerKey.getSignedPublicKey().getEncoded());
        ProtocolUtils.writeByteArray(buf, playerKey.getSignature());
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    // Accommodate the rare (but likely malicious) use of UTF-8 usernames, since it is technically
    // legal on the protocol level.
    int base = 1 + (16 * 4);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      return -1;
      //TODO: ## 19
    }
    return base;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
