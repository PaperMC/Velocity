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

package com.velocitypowered.proxy.protocol.packet.chat.keyed;

import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket.INVALID_PREVIOUS_MESSAGES;
import static com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket.MAXIMUM_PREVIOUS_MESSAGE_COUNT;

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KeyedPlayerCommandPacket implements MinecraftPacket {

  private static final int MAX_NUM_ARGUMENTS = 8;
  private static final int MAX_LENGTH_ARGUMENTS = 16;
  private static final QuietDecoderException LIMITS_VIOLATION =
      new QuietDecoderException("Command arguments incorrect size");

  private final boolean unsigned;
  private final String command;
  private final Instant timestamp;
  private final long salt;
  private final boolean signedPreview;
  private final SignaturePair[] previousMessages;
  private final @Nullable SignaturePair lastMessage;
  private final Map<String, byte[]> arguments;

  public KeyedPlayerCommandPacket(boolean unsigned, String command, Instant timestamp, long salt,
      boolean signedPreview, SignaturePair[] previousMessages, @Nullable SignaturePair lastMessage,
      Map<String, byte[]> arguments) {
    this.unsigned = unsigned;
    this.command = command;
    this.timestamp = timestamp;
    this.salt = salt;
    this.signedPreview = signedPreview;
    this.previousMessages = previousMessages;
    this.lastMessage = lastMessage;
    this.arguments = arguments;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public boolean isUnsigned() {
    return unsigned;
  }

  public String getCommand() {
    return command;
  }

  @Override
  public String toString() {
    return "PlayerCommand{"
        + "unsigned=" + unsigned
        + ", command='" + command + '\''
        + ", timestamp=" + timestamp
        + ", salt=" + salt
        + ", signedPreview=" + signedPreview
        + ", previousMessages=" + Arrays.toString(previousMessages)
        + ", arguments=" + arguments
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<KeyedPlayerCommandPacket> {
    @Override
    public KeyedPlayerCommandPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      String command = ProtocolUtils.readString(buf, 256);
      Instant timestamp = Instant.ofEpochMilli(buf.readLong());

      long salt = buf.readLong();

      int mapSize = ProtocolUtils.readVarInt(buf);
      if (mapSize > MAX_NUM_ARGUMENTS) {
        throw LIMITS_VIOLATION;
      }

      boolean unsigned = false;
      ImmutableMap.Builder<String, byte[]> entries = ImmutableMap.builderWithExpectedSize(mapSize);
      for (int i = 0; i < mapSize; i++) {
        entries.put(ProtocolUtils.readString(buf, MAX_LENGTH_ARGUMENTS),
            ProtocolUtils.readByteArray(buf, unsigned ? 0 : ProtocolUtils.DEFAULT_MAX_STRING_SIZE));
      }
      Map<String, byte[]> arguments = entries.build();

      boolean signedPreview = buf.readBoolean();
      if (unsigned && signedPreview) {
        throw EncryptionUtils.PREVIEW_SIGNATURE_MISSING;
      }

      SignaturePair[] previousMessages = new SignaturePair[0];
      SignaturePair lastMessage = null;

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        int size = ProtocolUtils.readVarInt(buf);
        if (size < 0 || size > MAXIMUM_PREVIOUS_MESSAGE_COUNT) {
          throw INVALID_PREVIOUS_MESSAGES;
        }

        SignaturePair[] lastSignatures = new SignaturePair[size];
        for (int i = 0; i < size; i++) {
          lastSignatures[i] = new SignaturePair(ProtocolUtils.readUuid(buf),
              ProtocolUtils.readByteArray(buf));
        }
        previousMessages = lastSignatures;

        if (buf.readBoolean()) {
          lastMessage = new SignaturePair(ProtocolUtils.readUuid(buf),
              ProtocolUtils.readByteArray(buf));
        }
      }

      if (salt == 0L && previousMessages.length == 0) {
        unsigned = true;
      }

      return new KeyedPlayerCommandPacket(unsigned, command, timestamp, salt, signedPreview,
          previousMessages, lastMessage, arguments);
    }

    @Override
    public void encode(KeyedPlayerCommandPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeString(buf, packet.command);
      buf.writeLong(packet.timestamp.toEpochMilli());

      buf.writeLong(packet.unsigned ? 0L : packet.salt);

      int size = packet.arguments.size();
      if (size > MAX_NUM_ARGUMENTS) {
        throw LIMITS_VIOLATION;
      }
      ProtocolUtils.writeVarInt(buf, size);
      for (Map.Entry<String, byte[]> entry : packet.arguments.entrySet()) {
        // What annoys me is that this isn't "sorted"
        ProtocolUtils.writeString(buf, entry.getKey());
        ProtocolUtils.writeByteArray(buf, packet.unsigned ? EncryptionUtils.EMPTY : entry.getValue());
      }

      buf.writeBoolean(packet.signedPreview);

      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
        ProtocolUtils.writeVarInt(buf, packet.previousMessages.length);
        for (SignaturePair previousMessage : packet.previousMessages) {
          ProtocolUtils.writeUuid(buf, previousMessage.getSigner());
          ProtocolUtils.writeByteArray(buf, previousMessage.getSignature());
        }

        if (packet.lastMessage != null) {
          buf.writeBoolean(true);
          ProtocolUtils.writeUuid(buf, packet.lastMessage.getSigner());
          ProtocolUtils.writeByteArray(buf, packet.lastMessage.getSignature());
        } else {
          buf.writeBoolean(false);
        }
      }
    }
  }
}
