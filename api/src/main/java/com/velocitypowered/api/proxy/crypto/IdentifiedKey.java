/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import java.security.PublicKey;

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
   * @param signature the signature data
   * @param toVerify the signed data
   *
   * @return validity of the signature
   */
  boolean verifyDataSignature(byte[] signature, byte[]... toVerify);

}
