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
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import io.netty.buffer.ByteBuf;
import java.time.Instant;

public class SessionPlayerChatPacket implements MinecraftPacket {

  protected String message;
  protected Instant timestamp;
  protected long salt;
  protected boolean signed;
  protected byte[] signature;
  protected LastSeenMessages lastSeenMessages;

  public SessionPlayerChatPacket() {
  }

  public String getMessage() {
    return message;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public long getSalt() {
    return salt;
  }

  public boolean isSigned() {
    return signed;
  }

  public byte[] getSignature() {
    return signature;
  }

  public LastSeenMessages getLastSeenMessages() {
    return lastSeenMessages;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    this.message = ProtocolUtils.readString(buf, 256);
    this.timestamp = Instant.ofEpochMilli(buf.readLong());
    this.salt = buf.readLong();
    this.signed = buf.readBoolean();
    if (this.signed) {
      this.signature = readMessageSignature(buf);
    } else {
      this.signature = new byte[0];
    }
    this.lastSeenMessages = new LastSeenMessages(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.message);
    buf.writeLong(this.timestamp.toEpochMilli());
    buf.writeLong(this.salt);
    buf.writeBoolean(this.signed);
    if (this.signed) {
      buf.writeBytes(this.signature);
    }
    this.lastSeenMessages.encode(buf);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  protected static byte[] readMessageSignature(ByteBuf buf) {
    byte[] signature = new byte[256];
    buf.readBytes(signature);
    return signature;
  }
}
