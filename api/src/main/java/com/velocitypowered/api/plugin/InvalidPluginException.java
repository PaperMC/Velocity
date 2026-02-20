/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

/**
 * Thrown if a JAR in the plugin directory does not look valid.
 */
public class InvalidPluginException extends Exception {

  /**
   * Creates a new exception with no detail message.
   */
  public InvalidPluginException() {
    super();
  }

  /**
   * Creates a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidPluginException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public InvalidPluginException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new exception with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public InvalidPluginException(Throwable cause) {
    super(cause);
  }
}
