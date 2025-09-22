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

package com.velocitypowered.proxy.crypto;

import com.google.common.base.Objects;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the contents of a {@link IdentifiedKey}.
 */
public class IdentifiedKeyImpl implements IdentifiedKey {

  private final Revision revision;
  private final PublicKey publicKey;
  private final byte[] signature;
  private final Instant expiryTemporal;
  private @MonotonicNonNull Boolean isSignatureValid;
  private @MonotonicNonNull UUID holder;

  public IdentifiedKeyImpl(Revision revision, byte[] keyBits, long expiry,
      byte[] signature) {
    this(revision, EncryptionUtils.parseRsaPublicKey(keyBits),
        Instant.ofEpochMilli(expiry), signature);
  }

  /**
   * Creates an Identified key from data.
   */
  public IdentifiedKeyImpl(
      Revision revision, PublicKey publicKey, Instant expiryTemporal, byte[] signature) {
    this.revision = revision;
    this.publicKey = publicKey;
    this.expiryTemporal = expiryTemporal;
    this.signature = signature;
  }

  @Override
  public PublicKey getSignedPublicKey() {
    return publicKey;
  }

  @Override
  public PublicKey getSigner() {
    return EncryptionUtils.getYggdrasilSessionKey();
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
  public @Nullable UUID getSignatureHolder() {
    return holder;
  }

  @Override
  public Revision getKeyRevision() {
    return revision;
  }

  /**
   * Sets the uuid for this key. Returns false if incorrect.
   */
  public boolean internalAddHolder(UUID holder) {
    if (holder == null) {
      return false;
    }
    if (this.holder == null) {
      Boolean result = validateData(holder);
      if (result == null || !result) {
        return false;
      }
      isSignatureValid = true;
      this.holder = holder;
      return true;
    }
    return this.holder.equals(holder) && isSignatureValid();
  }

  @Override
  public boolean isSignatureValid() {
    if (isSignatureValid == null) {
      isSignatureValid = validateData(holder);
    }
    return isSignatureValid != null && isSignatureValid;
  }

  private Boolean validateData(@Nullable UUID verify) {
    if (revision == Revision.GENERIC_V1) {
      String pemKey = EncryptionUtils.pemEncodeRsaKey(publicKey);
      long expires = expiryTemporal.toEpochMilli();
      byte[] toVerify = (expires + pemKey).getBytes(StandardCharsets.US_ASCII);
      return EncryptionUtils.verifySignature(
          EncryptionUtils.SHA1_WITH_RSA, EncryptionUtils.getYggdrasilSessionKey(), signature,
          toVerify);
    } else {
      if (verify == null) {
        return null;
      }
      byte[] keyBytes = publicKey.getEncoded();
      byte[] toVerify = new byte[keyBytes.length + 24]; // length long * 3
      ByteBuffer fixedDataSet = ByteBuffer.wrap(toVerify).order(ByteOrder.BIG_ENDIAN);
      fixedDataSet.putLong(verify.getMostSignificantBits());
      fixedDataSet.putLong(verify.getLeastSignificantBits());
      fixedDataSet.putLong(expiryTemporal.toEpochMilli());
      fixedDataSet.put(keyBytes);
      return EncryptionUtils.verifySignature(EncryptionUtils.SHA1_WITH_RSA,
          EncryptionUtils.getYggdrasilSessionKey(), signature, toVerify);
    }
  }

  @Override
  public boolean verifyDataSignature(byte[] signature, byte[]... toVerify) {
    try {
      return EncryptionUtils.verifySignature(EncryptionUtils.SHA256_WITH_RSA, publicKey, signature,
          toVerify);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "IdentifiedKeyImpl{"
        + "revision=" + revision
        + ", publicKey=" + publicKey
        + ", signature=" + Arrays.toString(signature)
        + ", expiryTemporal=" + expiryTemporal
        + ", isSignatureValid=" + isSignatureValid
        + ", holder=" + holder
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final IdentifiedKey that)) {
      return false;
    }

    return Objects.equal(this.getSignedPublicKey(), that.getSignedPublicKey())
        && Objects.equal(this.getExpiryTemporal(), that.getExpiryTemporal())
        && Arrays.equals(this.getSignature(), that.getSignature())
        && Objects.equal(this.getSigner(), that.getSigner());
  }
}
