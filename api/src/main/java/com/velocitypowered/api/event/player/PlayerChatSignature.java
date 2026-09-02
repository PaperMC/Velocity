/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable protocol signature fields from the original player-chat packet.
 *
 * @since 3.6.0
 */
public final class PlayerChatSignature {

  private final byte[] signature;
  private final @Nullable Instant timestamp;
  private final @Nullable Long salt;
  private final @Nullable byte[] saltBytes;
  private final boolean previewSigned;

  /**
   * Creates signature metadata.
   *
   * @param signature the raw signature bytes
   * @param timestamp the packet timestamp, if the protocol supplied one
   * @param salt the packet salt, if the protocol supplied one
   * @param saltBytes the packet salt as bytes, if available
   * @param previewSigned whether the signature applied to preview/styled content
   */
  public PlayerChatSignature(byte[] signature, @Nullable Instant timestamp, @Nullable Long salt,
      @Nullable byte[] saltBytes, boolean previewSigned) {
    this.signature = Preconditions.checkNotNull(signature, "signature").clone();
    this.timestamp = timestamp;
    this.salt = salt;
    this.saltBytes = saltBytes == null ? null : saltBytes.clone();
    this.previewSigned = previewSigned;
  }

  /**
   * Returns the raw packet signature bytes.
   *
   * @return the signature bytes
   */
  public byte[] getSignature() {
    return signature.clone();
  }

  /**
   * Returns the packet timestamp.
   *
   * @return the timestamp, if supplied by the protocol
   */
  public Optional<Instant> getTimestamp() {
    return Optional.ofNullable(timestamp);
  }

  /**
   * Returns the numeric packet salt.
   *
   * @return the salt, if supplied by the protocol
   */
  public Optional<Long> getSalt() {
    return Optional.ofNullable(salt);
  }

  /**
   * Returns the packet salt bytes.
   *
   * @return the salt bytes, if supplied by the protocol
   */
  public Optional<byte[]> getSaltBytes() {
    return saltBytes == null ? Optional.empty() : Optional.of(saltBytes.clone());
  }

  /**
   * Returns whether this signature applied to preview/styled content.
   *
   * @return whether preview content was signed
   */
  public boolean isPreviewSigned() {
    return previewSigned;
  }
}
