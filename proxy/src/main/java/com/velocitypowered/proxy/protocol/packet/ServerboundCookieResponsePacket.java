/*
 * Copyright (C) 2024 Velocity Contributors
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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundCookieResponsePacket implements MinecraftPacket {

  private Key key;
  private byte @Nullable [] payload;

  public Key getKey() {
    return key;
  }

  public byte @Nullable [] getPayload() {
    return payload;
  }

  public ServerboundCookieResponsePacket() {
  }

  public ServerboundCookieResponsePacket(final Key key, final byte @Nullable [] payload) {
    this.key = key;
    this.payload = payload;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.key = ProtocolUtils.readKey(buf);
    if (buf.readBoolean()) {
      this.payload = ProtocolUtils.readByteArray(buf, 5120);
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeKey(buf, key);
    final boolean hasPayload = payload != null && payload.length > 0;
    buf.writeBoolean(hasPayload);
    if (hasPayload) {
      ProtocolUtils.writeByteArray(buf, payload);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
