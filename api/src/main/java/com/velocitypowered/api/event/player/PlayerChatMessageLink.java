/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import java.util.UUID;

/**
 * A signed message reference carried by older message-chain protocol fields.
 *
 * @since 3.6.0
 */
public final class PlayerChatMessageLink {

  private final UUID signer;
  private final byte[] signature;

  /**
   * Creates a message-chain link.
   *
   * @param signer the referenced signer UUID
   * @param signature the referenced signature bytes
   */
  public PlayerChatMessageLink(UUID signer, byte[] signature) {
    this.signer = Preconditions.checkNotNull(signer, "signer");
    this.signature = Preconditions.checkNotNull(signature, "signature").clone();
  }

  /**
   * Returns the referenced signer UUID.
   *
   * @return the signer UUID
   */
  public UUID getSigner() {
    return signer;
  }

  /**
   * Returns the referenced signature bytes.
   *
   * @return the signature bytes
   */
  public byte[] getSignature() {
    return signature.clone();
  }
}
