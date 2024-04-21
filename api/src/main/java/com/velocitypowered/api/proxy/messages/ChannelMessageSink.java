/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents something that can be sent plugin messages.
 */
public interface ChannelMessageSink {

  /**
   * Sends a plugin message to this target.
   *
   * @param identifier the channel identifier to send the message on
   * @param data the data to send
   * @return whether or not the message could be sent
   */
  boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, byte @NotNull[] data);

  /**
   * Sends a plugin message to this target.
   *
   * <pre>
   *   final ChannelMessageSink target;
   *   final ChannelIdentifier identifier;
   *   final boolean result = target.sendPluginMessage(identifier, (output) -> {
   *     output.writeUTF("some input");
   *     output.writeInt(1);
   *   });
   * </pre>
   *
   * @param identifier the channel identifier to send the message on
   * @param dataEncoder the encoder of the data to be sent
   * @return whether the message could be sent
   */
  @ApiStatus.Experimental
  boolean sendPluginMessage(
          @NotNull ChannelIdentifier identifier,
          @NotNull PluginMessageEncoder dataEncoder);
}
