/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

import com.google.common.collect.Lists;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.List;

public class SessionPlayerCommand implements MinecraftPacket {

  protected String command;
  protected Instant timeStamp;
  protected long salt;
  protected ArgumentSignatures argumentSignatures;
  protected LastSeenMessages lastSeenMessages;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    this.command = ProtocolUtils.readString(buf, 256);
    this.timeStamp = Instant.ofEpochMilli(buf.readLong());
    this.salt = buf.readLong();
    this.argumentSignatures = new ArgumentSignatures(buf);
    this.lastSeenMessages = new LastSeenMessages(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.command);
    buf.writeLong(this.timeStamp.toEpochMilli());
    buf.writeLong(this.salt);
    this.argumentSignatures.encode(buf);
    this.lastSeenMessages.encode(buf);
  }

  public String getCommand() {
    return command;
  }

  public Instant getTimeStamp() {
    return timeStamp;
  }

  public boolean isSigned() {
    if (salt == 0) return false;
    return !lastSeenMessages.isEmpty() || !argumentSignatures.isEmpty();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "SessionPlayerCommand{" +
            "command='" + command + '\'' +
            ", timeStamp=" + timeStamp +
            ", salt=" + salt +
            ", argumentSignatures=" + argumentSignatures +
            ", lastSeenMessages=" + lastSeenMessages +
            '}';
  }

  public static class ArgumentSignatures {

    private final List<ArgumentSignature> entries;

    public ArgumentSignatures() {
      this.entries = List.of();
    }

    public ArgumentSignatures(ByteBuf buf) {
      int size = ProtocolUtils.readVarInt(buf);
      if (size > 8) {
        throw new QuietDecoderException(
            String.format("Too many argument signatures, %d is above limit %d", size, 8));
      }

      this.entries = Lists.newArrayListWithCapacity(size);
      for (int i = 0; i < size; i++) {
        this.entries.add(new ArgumentSignature(buf));
      }
    }

    public boolean isEmpty() {
      return this.entries.isEmpty();
    }

    public void encode(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, entries.size());
      for (ArgumentSignature entry : entries) {
        entry.encode(buf);
      }
    }
    @Override
    public String toString() {
      return "ArgumentSignatures{" +
              "entries=" + entries +
              '}';
    }
  }

  public static class ArgumentSignature {

    private final String name;
    private final byte[] signature;

    public ArgumentSignature(ByteBuf buf) {
      name = ProtocolUtils.readString(buf, 16);
      signature = SessionPlayerChat.readMessageSignature(buf);
    }

    public void encode(ByteBuf buf) {
      ProtocolUtils.writeString(buf, name);
      buf.writeBytes(signature);
    }

    @Override
    public String toString() {
      return "ArgumentSignature{" +
              "name='" + name + '\'' +
              '}';
    }
  }
}
