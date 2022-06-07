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

package com.velocitypowered.proxy.crypto;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SignedChatMessage implements SignedMessage {

  private static final QuietDecoderException INVALID_SIGNED_CHAT =
      new QuietDecoderException("Couldn't parse chat message");

  public static final TemporalAmount EXPIRY_TIME = Duration.ofMinutes(2L);

  private final String message;
  private final PublicKey signer;
  private final byte[] signature;
  private final Instant expiry;
  private final byte[] salt;
  private final UUID sender;
  //private final boolean isValid;
  private final boolean isPreviewSigned;

  /**
   * Create a signed message from data.
   */
  public SignedChatMessage(String message, PublicKey signer, UUID sender,
                           Instant expiry, byte[] signature, byte[] salt, boolean isPreviewSigned) {
    this.message = Preconditions.checkNotNull(message);
    this.signer = Preconditions.checkNotNull(signer);
    this.sender = Preconditions.checkNotNull(sender);
    this.signature = Preconditions.checkNotNull(signature);
    this.expiry = Preconditions.checkNotNull(expiry);
    this.salt = Preconditions.checkNotNull(salt);
    this.isPreviewSigned = isPreviewSigned;


    //this.isValid = EncryptionUtils.verifySignature(EncryptionUtils.SHA1_WITH_RSA, signer,
    //        signature, salt, EncryptionUtils.longToBigEndianByteArray(
    //                sender.getMostSignificantBits(), sender.getLeastSignificantBits()
    //        ), Longs.toByteArray(expiry.getEpochSecond()), message.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public PublicKey getSigner() {
    return signer;
  }

  @Override
  public Instant getExpiryTemporal() {
    return expiry;
  }

  @Override
  public @Nullable byte[] getSignature() {
    return signature;
  }

  //@Override
  //public boolean isSignatureValid() {
  //  return isValid;
  //}

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public UUID getSignerUuid() {
    return sender;
  }

  @Override
  public boolean isPreviewSigned() {
    return isPreviewSigned;
  }

  @Override
  public byte[] getSalt() {
    return salt;
  }

}
