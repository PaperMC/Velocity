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

/**
 * Represents a player chat packet specific to a session, implementing {@link MinecraftPacket}.
 *
 * <p>The {@code SessionPlayerChatPacket} handles chat messages sent by a player during a session,
 * and may include session-specific context, such as timestamps, message formatting, or other
 * relevant session data.</p>
 */
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
    this.lastSeenMessages = new LastSeenMessages(buf, protocolVersion);
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
    this.lastSeenMessages.encode(buf, protocolVersion);
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

  /**
   * Creates a new {@code SessionPlayerChatPacket} with the specified last-seen messages.
   *
   * <p>This method constructs a new {@code SessionPlayerChatPacket} instance that retains the
   * current packet's properties, while updating the last seen messages.</p>
   *
   * @param lastSeenMessages the last seen messages to associate with the new packet
   * @return a new {@code SessionPlayerChatPacket} with the updated last seen messages
   */
  public SessionPlayerChatPacket withLastSeenMessages(LastSeenMessages lastSeenMessages) {
    SessionPlayerChatPacket packet = new SessionPlayerChatPacket();
    packet.message = message;
    packet.timestamp = timestamp;
    packet.salt = salt;
    packet.signed = signed;
    packet.signature = signature;
    packet.lastSeenMessages = lastSeenMessages;
    return packet;
  }
}
