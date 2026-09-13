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
 * Immutable modern Minecraft chat-session metadata associated with a player-chat message.
 *
 * @since 3.6.0
 */
public final class PlayerChatSessionInfo {

  private final UUID sessionId;
  private final PublicKey publicKey;
  private final Instant keyExpiry;
  private final @Nullable UUID keyHolder;

  /**
   * Creates session metadata.
   *
   * @param sessionId the chat session UUID
   * @param publicKey the session public key
   * @param keyExpiry the session-server key expiry
   * @param keyHolder the key holder UUID, if known
   */
  public PlayerChatSessionInfo(UUID sessionId, PublicKey publicKey, Instant keyExpiry,
      @Nullable UUID keyHolder) {
    this.sessionId = Preconditions.checkNotNull(sessionId, "sessionId");
    this.publicKey = Preconditions.checkNotNull(publicKey, "publicKey");
    this.keyExpiry = Preconditions.checkNotNull(keyExpiry, "keyExpiry");
    this.keyHolder = keyHolder;
  }

  /**
   * Returns the chat session UUID.
   *
   * @return the session UUID
   */
  public UUID getSessionId() {
    return sessionId;
  }

  /**
   * Returns the public key associated with this chat session.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns the session-server expiry associated with the public key.
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
