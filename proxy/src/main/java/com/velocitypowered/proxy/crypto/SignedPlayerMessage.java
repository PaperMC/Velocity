/*
 * Copyright (C) 2026 Velocity Contributors
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
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable representation of a client-submitted signed player chat message.
 */
public final class SignedPlayerMessage implements SignedMessage {

  private final String message;
  private final PublicKey signer;
  private final UUID signerUuid;
  private final Instant expiryTemporal;
  private final byte[] signature;
  private final @Nullable byte[] salt;
  private final boolean previewSigned;

  /**
   * Creates an immutable signed player message from protocol data.
   *
   * @param message the original signed message body
   * @param signer the public key that signed the message
   * @param signerUuid the player UUID that signed the message
   * @param expiryTemporal the expiry time associated with the signing key or message
   * @param signature the message signature
   * @param salt the message salt, if present
   * @param previewSigned whether the signature applies to a signed preview
   */
  public SignedPlayerMessage(String message, PublicKey signer, UUID signerUuid,
      Instant expiryTemporal, byte[] signature, @Nullable byte[] salt, boolean previewSigned) {
    this.message = Preconditions.checkNotNull(message, "message");
    this.signer = Preconditions.checkNotNull(signer, "signer");
    this.signerUuid = Preconditions.checkNotNull(signerUuid, "signerUuid");
    this.expiryTemporal = Preconditions.checkNotNull(expiryTemporal, "expiryTemporal");
    this.signature = Preconditions.checkNotNull(signature, "signature").clone();
    this.salt = salt == null ? null : salt.clone();
    this.previewSigned = previewSigned;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public UUID getSignerUuid() {
    return signerUuid;
  }

  @Override
  public boolean isPreviewSigned() {
    return previewSigned;
  }

  @Override
  public PublicKey getSigner() {
    return signer;
  }

  @Override
  public Instant getExpiryTemporal() {
    return expiryTemporal;
  }

  @Override
  public byte[] getSignature() {
    return signature.clone();
  }

  @Override
  public @Nullable byte[] getSalt() {
    return salt == null ? null : salt.clone();
  }
}
