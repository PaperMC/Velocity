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
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import io.netty.buffer.ByteBuf;
import java.time.Instant;

public class SessionPlayerChatPacket implements MinecraftPacket {

  protected final String message;
  protected final Instant timestamp;
  protected final long salt;
  protected final boolean signed;
  protected final byte[] signature;
  protected final LastSeenMessages lastSeenMessages;

  public SessionPlayerChatPacket(String message, Instant timestamp, long salt, boolean signed,
      byte[] signature, LastSeenMessages lastSeenMessages) {
    this.message = message;
    this.timestamp = timestamp;
    this.salt = salt;
    this.signed = signed;
    this.signature = signature;
    this.lastSeenMessages = lastSeenMessages;
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
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  protected static byte[] readMessageSignature(ByteBuf buf) {
    byte[] signature = new byte[256];
    buf.readBytes(signature);
    return signature;
  }

  public SessionPlayerChatPacket withLastSeenMessages(LastSeenMessages lastSeenMessages) {
    return new SessionPlayerChatPacket(message, timestamp, salt, signed, signature, lastSeenMessages);
  }

  public static class Codec implements PacketCodec<SessionPlayerChatPacket> {
    @Override
    public SessionPlayerChatPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      String message = ProtocolUtils.readString(buf, 256);
      Instant timestamp = Instant.ofEpochMilli(buf.readLong());
      long salt = buf.readLong();
      boolean signed = buf.readBoolean();
      byte[] signature;
      if (signed) {
        signature = readMessageSignature(buf);
      } else {
        signature = new byte[0];
      }
      LastSeenMessages lastSeenMessages = new LastSeenMessages(buf, protocolVersion);
      return new SessionPlayerChatPacket(message, timestamp, salt, signed, signature, lastSeenMessages);
    }

    @Override
    public void encode(SessionPlayerChatPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeString(buf, packet.message);
      buf.writeLong(packet.timestamp.toEpochMilli());
      buf.writeLong(packet.salt);
      buf.writeBoolean(packet.signed);
      if (packet.signed) {
        buf.writeBytes(packet.signature);
      }
      packet.lastSeenMessages.encode(buf, protocolVersion);
    }
  }
}
