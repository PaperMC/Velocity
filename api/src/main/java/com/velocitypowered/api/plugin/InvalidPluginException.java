/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

public class InvalidPluginException extends Exception {

  public InvalidPluginException() {
    super();
  }

  public InvalidPluginException(String message) {
    super(message);
  }

  public InvalidPluginException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidPluginException(Throwable cause) {
    super(cause);
  }
}
