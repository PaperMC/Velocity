/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import java.util.UUID;

public interface SignedMessage extends KeySigned {

  /**
   * Returns the signed message.
   *
   * @return the message
   */
  String getMessage();

  /**
   * Returns the signers UUID.
   * Can be a player UUID or a self-signed UUID.
   *
   * @return the uuid
   */
  UUID getSignerUuid();

}
