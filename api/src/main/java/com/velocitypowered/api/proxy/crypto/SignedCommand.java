/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import java.util.List;

public interface SignedCommand extends KeySigned {

  /**
   * Retrieves the unsigned base command.
   *
   * @return the command
   */
  String getUnsignedCommand();

  /**
   * Retrieves the unsigned base command.
   *
   * @return the signed arguments
   */
  List<SignedMessage> getSignedArguments();
}
