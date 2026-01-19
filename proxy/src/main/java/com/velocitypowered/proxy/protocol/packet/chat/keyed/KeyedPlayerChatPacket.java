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

import com.google.common.primitives.Longs;
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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KeyedPlayerChatPacket implements MinecraftPacket {

  public static final int MAXIMUM_PREVIOUS_MESSAGE_COUNT = 5;

  public static final QuietDecoderException INVALID_PREVIOUS_MESSAGES =
      new QuietDecoderException("Invalid previous messages");

  private final String message;
  private final boolean signedPreview;
  private final boolean unsigned;
  private final @Nullable Instant expiry;
  private final @Nullable byte[] signature;
  private final @Nullable byte[] salt;
  private final SignaturePair[] previousMessages;
  private final @Nullable SignaturePair lastMessage;

  public KeyedPlayerChatPacket(String message, boolean signedPreview, boolean unsigned,
      @Nullable Instant expiry, @Nullable byte[] signature, @Nullable byte[] salt,
      SignaturePair[] previousMessages, @Nullable SignaturePair lastMessage) {
    this.message = message;
    this.signedPreview = signedPreview;
    this.unsigned = unsigned;
    this.expiry = expiry;
    this.signature = signature;
    this.salt = salt;
    this.previousMessages = previousMessages;
    this.lastMessage = lastMessage;
  }

  public @Nullable Instant getExpiry() {
    return expiry;
  }

  public boolean isUnsigned() {
    return unsigned;
  }

  public String getMessage() {
    return message;
  }

  public boolean isSignedPreview() {
    return signedPreview;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<KeyedPlayerChatPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public KeyedPlayerChatPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      String message = ProtocolUtils.readString(buf, 256);

      long expiresAt = buf.readLong();
      long saltLong = buf.readLong();
      byte[] signatureBytes = ProtocolUtils.readByteArray(buf);

      byte[] salt = null;
      byte[] signature = null;
      Instant expiry = null;
      boolean unsigned;

      if (saltLong != 0L && signatureBytes.length > 0) {
        salt = Longs.toByteArray(saltLong);
        signature = signatureBytes;
        expiry = Instant.ofEpochMilli(expiresAt);
        unsigned = false;
      } else if ((protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)
          || saltLong == 0L) && signatureBytes.length == 0) {
        unsigned = true;
      } else {
        throw EncryptionUtils.INVALID_SIGNATURE;
      }

      boolean signedPreview = buf.readBoolean();
      if (signedPreview && unsigned) {
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

      return new KeyedPlayerChatPacket(message, signedPreview, unsigned, expiry, signature, salt,
          previousMessages, lastMessage);
    }

    @Override
    public void encode(KeyedPlayerChatPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeString(buf, packet.message);

      buf.writeLong(packet.unsigned ? Instant.now().toEpochMilli() : packet.expiry.toEpochMilli());
      buf.writeLong(packet.unsigned ? 0L : Longs.fromByteArray(packet.salt));

      ProtocolUtils.writeByteArray(buf, packet.unsigned ? EncryptionUtils.EMPTY : packet.signature);

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
