/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable legacy keyed-chat public-key metadata associated with a message.
 *
 * @since 3.6.0
 */
public final class PlayerChatKeyInfo {

  private final PublicKey publicKey;
  private final Instant keyExpiry;
  private final @Nullable UUID keyHolder;

  /**
   * Creates keyed-chat metadata.
   *
   * @param publicKey the signing public key
   * @param keyExpiry the key expiry
   * @param keyHolder the key holder UUID, if known
   */
  public PlayerChatKeyInfo(PublicKey publicKey, Instant keyExpiry, @Nullable UUID keyHolder) {
    this.publicKey = Preconditions.checkNotNull(publicKey, "publicKey");
    this.keyExpiry = Preconditions.checkNotNull(keyExpiry, "keyExpiry");
    this.keyHolder = keyHolder;
  }

  /**
   * Returns the signing public key.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns the key expiry.
   *
   * @return the key expiry
   */
  public Instant getKeyExpiry() {
    return keyExpiry;
  }

  /**
   * Returns the UUID embedded in the identified key, if present.
   *
   * @return the key holder UUID, if known
   */
  public Optional<UUID> getKeyHolder() {
    return Optional.ofNullable(keyHolder);
  }
}
