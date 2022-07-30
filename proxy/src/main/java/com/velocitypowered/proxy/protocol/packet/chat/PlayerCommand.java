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

package com.velocitypowered.proxy.protocol.packet.chat;

import static com.velocitypowered.proxy.protocol.packet.chat.PlayerChat.INVALID_PREVIOUS_MESSAGES;
import static com.velocitypowered.proxy.protocol.packet.chat.PlayerChat.MAXIMUM_PREVIOUS_MESSAGE_COUNT;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.crypto.SignedChatCommand;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerCommand implements MinecraftPacket {

  private static final int MAX_NUM_ARGUMENTS = 8;
  private static final int MAX_LENGTH_ARGUMENTS = 16;
  private static final QuietDecoderException LIMITS_VIOLATION =
      new QuietDecoderException("Command arguments incorrect size");

  private boolean unsigned = false;
  private String command;
  private Instant timestamp;
  private long salt;
  private boolean signedPreview; // Good god. Please no.
  private SignaturePair[] previousMessages = new SignaturePair[0];
  private @Nullable SignaturePair lastMessage;
  private Map<String, byte[]> arguments = ImmutableMap.of();

  public boolean isSignedPreview() {
    return signedPreview;
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

  public PlayerCommand() {
  }

  /**
   * Creates an {@link PlayerCommand} packet based on a command and list of arguments.
   *
   * @param command the command to run
   * @param arguments the arguments of the command
   * @param timestamp the timestamp of the command execution
   */
  public PlayerCommand(String command, List<String> arguments, Instant timestamp) {
    this.unsigned = true;
    ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();
    arguments.forEach(entry -> builder.put(entry, EncryptionUtils.EMPTY));
    this.arguments = builder.build();
    this.timestamp = timestamp;
    this.command = command;
    this.signedPreview = false;
    this.salt = 0L;
  }

  /**
   * Create new {@link PlayerCommand} based on a previously {@link SignedChatCommand}.
   *
   * @param signedCommand The {@link SignedChatCommand} to turn into {@link PlayerCommand}.
   */
  public PlayerCommand(SignedChatCommand signedCommand) {
    this.command = signedCommand.getBaseCommand();
    this.arguments = ImmutableMap.copyOf(signedCommand.getSignatures());
    this.timestamp = signedCommand.getExpiryTemporal();
    this.salt = Longs.fromByteArray(signedCommand.getSalt());
    this.signedPreview = signedCommand.isPreviewSigned();
    this.lastMessage = signedCommand.getLastSignature();
    this.previousMessages = signedCommand.getPreviousSignatures();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    command = ProtocolUtils.readString(buf, 256);
    timestamp = Instant.ofEpochMilli(buf.readLong());

    salt = buf.readLong();
    if (salt == 0L) {
      unsigned = true;
    }

    int mapSize = ProtocolUtils.readVarInt(buf);
    if (mapSize > MAX_NUM_ARGUMENTS) {
      throw LIMITS_VIOLATION;
    }
    // Mapped as Argument : signature
    ImmutableMap.Builder<String, byte[]> entries = ImmutableMap.builderWithExpectedSize(mapSize);
    for (int i = 0; i < mapSize; i++) {
      entries.put(ProtocolUtils.readString(buf, MAX_LENGTH_ARGUMENTS),
          ProtocolUtils.readByteArray(buf, unsigned ? 0 : ProtocolUtils.DEFAULT_MAX_STRING_SIZE));
    }
    arguments = entries.build();

    signedPreview = buf.readBoolean();
    if (unsigned && signedPreview) {
      throw EncryptionUtils.PREVIEW_SIGNATURE_MISSING;
    }

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
      int size = ProtocolUtils.readVarInt(buf);
      if (size < 0 || size > MAXIMUM_PREVIOUS_MESSAGE_COUNT) {
        throw INVALID_PREVIOUS_MESSAGES;
      }

      SignaturePair[] lastSignatures = new SignaturePair[size];
      for (int i = 0; i < size; i++) {
        lastSignatures[i] = new SignaturePair(ProtocolUtils.readUuid(buf), ProtocolUtils.readByteArray(buf));
      }
      previousMessages = lastSignatures;

      if (buf.readBoolean()) {
        lastMessage = new SignaturePair(ProtocolUtils.readUuid(buf), ProtocolUtils.readByteArray(buf));
      }
    }

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, command);
    buf.writeLong(timestamp.toEpochMilli());

    buf.writeLong(unsigned ? 0L : salt);

    int size = arguments.size();
    if (size > MAX_NUM_ARGUMENTS) {
      throw LIMITS_VIOLATION;
    }
    ProtocolUtils.writeVarInt(buf, size);
    for (Map.Entry<String, byte[]> entry : arguments.entrySet()) {
      // What annoys me is that this isn't "sorted"
      ProtocolUtils.writeString(buf, entry.getKey());
      ProtocolUtils.writeByteArray(buf, unsigned ? EncryptionUtils.EMPTY : entry.getValue());
    }

    buf.writeBoolean(signedPreview);

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
      ProtocolUtils.writeVarInt(buf, previousMessages.length);
      for (SignaturePair previousMessage : previousMessages) {
        ProtocolUtils.writeUuid(buf, previousMessage.getSigner());
        ProtocolUtils.writeByteArray(buf, previousMessage.getSignature());
      }

      if (lastMessage != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeUuid(buf, lastMessage.getSigner());
        ProtocolUtils.writeByteArray(buf, lastMessage.getSignature());
      } else {
        buf.writeBoolean(false);
      }
    }

  }

  /**
   * Validates a signature and creates a {@link SignedChatCommand} from the given signature.
   *
   * @param signer the signer's information
   * @param sender the sender of the message
   * @param mustSign instructs the function to throw if the signature is invalid.
   * @return The {@link SignedChatCommand} or null if the signature couldn't be verified.
   * @throws com.velocitypowered.proxy.util.except.QuietDecoderException when mustSign is {@code true} and the signature
   *                                                                     is invalid.
   */
  public SignedChatCommand signedContainer(
      @Nullable IdentifiedKey signer, UUID sender, boolean mustSign) {
    // There's a certain mod that is very broken that still signs messages but
    // doesn't provide the player key. This is broken and wrong, but we need to
    // work around that.
    if (unsigned || signer == null) {
      if (mustSign) {
        throw EncryptionUtils.INVALID_SIGNATURE;
      }
      return null;
    }

    return new SignedChatCommand(command, signer.getSignedPublicKey(), sender, timestamp,
        arguments, Longs.toByteArray(salt), signedPreview, previousMessages, lastMessage);
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
}
