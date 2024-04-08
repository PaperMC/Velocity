/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import com.google.common.io.ByteArrayDataOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A data encoder to be sent via a plugin message.
 *
 * @since 3.3.0
 */
@FunctionalInterface
@ApiStatus.Experimental
public interface PluginMessageEncoder {

  /**
   * Encodes data into a {@link ByteArrayDataOutput} to be transmitted by plugin messages.
   *
   * @param output the {@link ByteArrayDataOutput} provided
   */
  void encode(@NotNull ByteArrayDataOutput output);
}
