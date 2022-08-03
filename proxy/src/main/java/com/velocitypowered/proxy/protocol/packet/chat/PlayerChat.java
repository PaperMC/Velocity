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

import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerChat implements MinecraftPacket {

  private String message;
  private boolean signedPreview;
  private boolean unsigned = false;
  private @Nullable Instant expiry;
  private @Nullable byte[] signature;
  private @Nullable byte[] salt;
  private SignaturePair[] previousMessages = new SignaturePair[0];
  private @Nullable SignaturePair lastMessage;

  public static final int MAXIMUM_PREVIOUS_MESSAGE_COUNT = 5;

  public static final QuietDecoderException INVALID_PREVIOUS_MESSAGES =
          new QuietDecoderException("Invalid previous messages");

  public PlayerChat() {
  }

  public PlayerChat(String message) {
    this.message = message;
    this.unsigned = true;
  }

  /**
   * Create new {@link PlayerChat} based on a previously {@link SignedChatMessage}.
   *
   * @param message The {@link SignedChatMessage} to turn into {@link PlayerChat}.
   */
  public PlayerChat(SignedChatMessage message) {
    this.message = message.getMessage();
    this.expiry = message.getExpiryTemporal();
    this.salt = message.getSalt();
    this.signature = message.getSignature();
    this.signedPreview = message.isPreviewSigned();
    this.lastMessage = message.getPreviousSignature();
    this.previousMessages = message.getPreviousSignatures();
  }

  public Instant getExpiry() {
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
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    message = ProtocolUtils.readString(buf, 256);

    long expiresAt = buf.readLong();
    long saltLong = buf.readLong();
    byte[] signatureBytes = ProtocolUtils.readByteArray(buf);

    if (saltLong != 0L && signatureBytes.length > 0) {
      salt = Longs.toByteArray(saltLong);
      signature = signatureBytes;
      expiry = Instant.ofEpochMilli(expiresAt);
    } else if ((protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0
        || saltLong == 0L) && signatureBytes.length == 0) {
      unsigned = true;
    } else {
      throw EncryptionUtils.INVALID_SIGNATURE;
    }

    signedPreview = buf.readBoolean();
    if (signedPreview && unsigned) {
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
    ProtocolUtils.writeString(buf, message);

    buf.writeLong(unsigned ? Instant.now().toEpochMilli() : expiry.toEpochMilli());
    buf.writeLong(unsigned ? 0L : Longs.fromByteArray(salt));
    ProtocolUtils.writeByteArray(buf, unsigned ? EncryptionUtils.EMPTY : signature);

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
   * Validates a signature and creates a {@link SignedChatMessage} from the given signature.
   *
   * @param signer the signer's information
   * @param sender the sender of the message
   * @param mustSign instructs the function to throw if the signature is invalid.
   * @return The {@link SignedChatMessage} or null if the signature couldn't be verified.
   * @throws com.velocitypowered.proxy.util.except.QuietDecoderException when mustSign is {@code true} and the signature
   *                                                                     is invalid.
   */
  public SignedChatMessage signedContainer(IdentifiedKey signer, UUID sender, boolean mustSign) {
    if (unsigned) {
      if (mustSign) {
        throw EncryptionUtils.INVALID_SIGNATURE;
      }
      return null;
    }

    return new SignedChatMessage(message, signer.getSignedPublicKey(), sender, expiry, signature,
            salt, signedPreview, previousMessages, lastMessage);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
