/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import net.kyori.adventure.text.Component;

public interface PreviewSigned extends KeySigned {

  /**
   * Returns the preview Component the message is signed against.
   *
   * @return the preview component
   */
  Component getPreview();


  /**
   * Represents the origin of the preview.
   */
  enum Origin {
    /**
     * Resource-pack originated from the downstream server.
     */
    DOWNSTREAM_SERVER,
    /**
     * The resource-pack originated from a plugin on this proxy.
     */
    PLUGIN_ON_PROXY
  }
}
