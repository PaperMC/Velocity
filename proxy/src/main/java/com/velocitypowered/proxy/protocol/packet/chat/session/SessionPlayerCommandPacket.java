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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a packet that handles player commands in a Minecraft session.
 *
 * <p>This packet contains information about the player's command, timestamp,
 * salt, argument signatures, and last seen messages for verification and
 * processing.</p>
 */
public class SessionPlayerCommandPacket implements MinecraftPacket {

  protected String command;
  protected Instant timeStamp;
  protected long salt;
  protected ArgumentSignatures argumentSignatures;
  protected LastSeenMessages lastSeenMessages;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int cap = protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_5) ? 256 : ProtocolUtils.DEFAULT_MAX_STRING_SIZE;
    this.command = ProtocolUtils.readString(buf, cap);
    this.timeStamp = Instant.ofEpochMilli(buf.readLong());
    this.salt = buf.readLong();
    this.argumentSignatures = new ArgumentSignatures(buf);
    this.lastSeenMessages = new LastSeenMessages(buf, protocolVersion);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, this.command);
    buf.writeLong(this.timeStamp.toEpochMilli());
    buf.writeLong(this.salt);
    this.argumentSignatures.encode(buf);
    this.lastSeenMessages.encode(buf, protocolVersion);
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
    return !this.argumentSignatures.isEmpty() ? CommandExecuteEvent.SignedState.SIGNED_WITH_ARGS
          : CommandExecuteEvent.SignedState.SIGNED_WITHOUT_ARGS;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "SessionPlayerCommand{"
        + "command='" + command + '\''
        + ", timeStamp=" + timeStamp
        + ", salt=" + salt
        + ", argumentSignatures=" + argumentSignatures
        + ", lastSeenMessages=" + lastSeenMessages
        + '}';
  }

  /**
   * Returns a new instance of {@code SessionPlayerCommandPacket} with the specified
   * {@code LastSeenMessages}.
   *
   * <p>If {@code lastSeenMessages} is null, it creates an {@code UnsignedPlayerCommandPacket}
   * instead. Otherwise, it creates a new {@code SessionPlayerCommandPacket} with the
   * provided {@code lastSeenMessages}.</p>
   *
   * @param lastSeenMessages the last seen messages to include in the packet, may be {@code null}
   * @return a new instance of {@code SessionPlayerCommandPacket} or {@code UnsignedPlayerCommandPacket}
   */
  public SessionPlayerCommandPacket withLastSeenMessages(@Nullable LastSeenMessages lastSeenMessages) {
    if (lastSeenMessages == null) {
      UnsignedPlayerCommandPacket packet = new UnsignedPlayerCommandPacket();
      packet.command = command;
      return packet;
    }
    SessionPlayerCommandPacket packet = new SessionPlayerCommandPacket();
    packet.command = command;
    packet.timeStamp = timeStamp;
    packet.salt = salt;
    packet.argumentSignatures = argumentSignatures;
    packet.lastSeenMessages = lastSeenMessages;
    return packet;
  }

  /**
   * Represents a collection of argument signatures for commands.
   *
   * <p>This class is responsible for handling the encoding and decoding of
   * argument signatures associated with a player command in a Minecraft session.</p>
   */
  public static class ArgumentSignatures {

    private final List<ArgumentSignature> entries;

    public ArgumentSignatures() {
      this.entries = List.of();
    }

    /**
     * Constructs an {@code ArgumentSignatures} instance by decoding the signatures
     * from the provided {@code ByteBuf}.
     *
     * <p>This constructor reads the argument signatures from the buffer and ensures
     * that the number of signatures does not exceed the allowed limit.</p>
     *
     * @param buf the {@code ByteBuf} to decode the argument signatures from
     * @throws QuietDecoderException if the number of argument signatures exceeds the allowed limit
     */
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

    /**
     * Encodes the argument signatures into the provided {@code ByteBuf}.
     *
     * <p>This method writes the number of argument signatures and each signature's
     * details into the buffer for transmission.</p>
     *
     * @param buf the {@code ByteBuf} to encode the argument signatures into
     */
    public void encode(ByteBuf buf) {
      ProtocolUtils.writeVarInt(buf, entries.size());
      for (ArgumentSignature entry : entries) {
        entry.encode(buf);
      }
    }

    /**
     * Returns a string representation of this {@code ArgumentSignatures} instance.
     *
     * <p>The output includes a list of individual {@link ArgumentSignature} entries
     * attached to this command packet.</p>
     *
     * @return a human-readable string describing the argument signatures
     */
    @Override
    public String toString() {
      return "ArgumentSignatures{"
          + "entries=" + entries
          + '}';
    }
  }

  /**
   * Represents a single argument signature associated with a command.
   *
   * <p>This class is responsible for handling the encoding and decoding of
   * individual argument signatures, which consist of a name and a signature
   * (byte array).</p>
   */
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
      return "ArgumentSignature{"
          + "name='" + name + '\''
          + '}';
    }
  }
}
