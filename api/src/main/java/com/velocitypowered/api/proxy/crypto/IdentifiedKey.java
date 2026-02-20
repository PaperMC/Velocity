/*
 * Copyright (C) 2022-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.Ordered;
import java.security.PublicKey;
import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents session-server cross-signed dated RSA public key.
 */
public interface IdentifiedKey extends KeySigned {

  /**
   * Returns RSA public key.
   * Note: this key is at least 2048 bits but may be larger.
   *
   * @return the RSA public key in question
   */
  PublicKey getSignedPublicKey();

  /**
   * Validates a signature against this public key.
   *
   * @param signature the signature data
   * @param toVerify the signed data
   *
   * @return validity of the signature
   */
  boolean verifyDataSignature(byte[] signature, byte[]... toVerify);

  /**
   * Retrieves the signature holders UUID.
   * Returns null before the {@link com.velocitypowered.api.event.connection.LoginEvent}.
   *
   * @return the holder UUID or null if not present
   */
  @Nullable
  UUID getSignatureHolder();

  /**
   * Retrieves the key revision.
   *
   * @return the key revision
   */
  Revision getKeyRevision();

  /**
   * The different versions of player keys, per Minecraft version.
   */
  enum Revision implements Ordered<Revision> {
    /**
     * Represents the original key revision introduced in Minecraft 1.19.
     * Keys are not tied to a specific player identity.
     */
    GENERIC_V1(ImmutableSet.of(), ImmutableSet.of(ProtocolVersion.MINECRAFT_1_19)),
    /**
     * Represents the key revision introduced in Minecraft 1.19.1.
     * Keys are cryptographically linked to player identities.
     */
    LINKED_V2(ImmutableSet.of(), ImmutableSet.of(ProtocolVersion.MINECRAFT_1_19_1));

    final Set<Revision> backwardsCompatibleTo;
    final Set<ProtocolVersion> applicableTo;

    Revision(Set<Revision> backwardsCompatibleTo, Set<ProtocolVersion> applicableTo) {
      this.backwardsCompatibleTo = backwardsCompatibleTo;
      this.applicableTo = applicableTo;
    }

    /**
     * Returns the set of revisions that this revision is backwards-compatible with.
     *
     * @return a set of compatible earlier revisions
     */
    public Set<Revision> getBackwardsCompatibleTo() {
      return backwardsCompatibleTo;
    }

    /**
     * Returns the set of Minecraft protocol versions this revision applies to.
     *
     * @return a set of applicable protocol versions
     */
    public Set<ProtocolVersion> getApplicableTo() {
      return applicableTo;
    }
  }

}
