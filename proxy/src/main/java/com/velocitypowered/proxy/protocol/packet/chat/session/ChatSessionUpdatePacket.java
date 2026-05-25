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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;

public class ChatSessionUpdatePacket implements MinecraftPacket {

  private UUID sessionId;
  private long expiresAt;
  private byte[] publicKey;
  private byte[] keySignature;

  public ChatSessionUpdatePacket() {
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.sessionId = ProtocolUtils.readUuid(buf);
    this.expiresAt = buf.readLong();
    this.publicKey = ProtocolUtils.readByteArray(buf, 512);
    this.keySignature = ProtocolUtils.readByteArray(buf, 4096);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeUuid(buf, sessionId);
    buf.writeLong(expiresAt);
    ProtocolUtils.writeByteArray(buf, publicKey);
    ProtocolUtils.writeByteArray(buf, keySignature);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
