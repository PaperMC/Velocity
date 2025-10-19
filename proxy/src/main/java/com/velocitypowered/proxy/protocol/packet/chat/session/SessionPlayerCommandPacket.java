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
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.List;

public class SessionPlayerCommandPacket implements MinecraftPacket {

  protected final String command;
  protected final Instant timeStamp;
  protected final long salt;
  protected final ArgumentSignatures argumentSignatures;
  protected final LastSeenMessages lastSeenMessages;

  public SessionPlayerCommandPacket(String command, Instant timeStamp, long salt,
      ArgumentSignatures argumentSignatures, LastSeenMessages lastSeenMessages) {
    this.command = command;
    this.timeStamp = timeStamp;
    this.salt = salt;
    this.argumentSignatures = argumentSignatures;
    this.lastSeenMessages = lastSeenMessages;
  }

  public String getCommand() {
    return command;
  }

  public Instant getTimeStamp() {
    return timeStamp;
  }

  public boolean isSigned() {
    return !argumentSignatures.isEmpty();
  }

  public CommandExecuteEvent.SignedState getEventSignedState() {
    return !this.argumentSignatures.isEmpty() ? CommandExecuteEvent.SignedState.SIGNED_WITH_ARGS : CommandExecuteEvent.SignedState.SIGNED_WITHOUT_ARGS;
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

  public SessionPlayerCommandPacket withLastSeenMessages(@Nullable LastSeenMessages lastSeenMessages) {
    if (lastSeenMessages == null) {
      return new UnsignedPlayerCommandPacket(command);
    }
    return new SessionPlayerCommandPacket(command, timeStamp, salt, argumentSignatures, lastSeenMessages);
  }

  public static class Codec implements PacketCodec<SessionPlayerCommandPacket> {
    @Override
    public SessionPlayerCommandPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      int cap = protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5) ? 256 : ProtocolUtils.DEFAULT_MAX_STRING_SIZE;
      String command = ProtocolUtils.readString(buf, cap);
      Instant timeStamp = Instant.ofEpochMilli(buf.readLong());
      long salt = buf.readLong();
      ArgumentSignatures argumentSignatures = new ArgumentSignatures(buf);
      LastSeenMessages lastSeenMessages = new LastSeenMessages(buf, protocolVersion);
      return new SessionPlayerCommandPacket(command, timeStamp, salt, argumentSignatures, lastSeenMessages);
    }

    @Override
    public void encode(SessionPlayerCommandPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeString(buf, packet.command);
      buf.writeLong(packet.timeStamp.toEpochMilli());
      buf.writeLong(packet.salt);
      packet.argumentSignatures.encode(buf);
      packet.lastSeenMessages.encode(buf, protocolVersion);
    }
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
      signature = SessionPlayerChatPacket.readMessageSignature(buf);
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
