/*
 * Copyright (C) 2021-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a connextion that is in the login phase. This is most useful in conjunction
 * for login plugin messages.
 */
public interface LoginPhaseConnection extends InboundConnection, KeyIdentifiable {

  /**
   * Sends a login plugin message to the client, and provides a consumer to react to the
   * response to the client. The login process will not continue until there are no more
   * login plugin messages that require responses.
   *
   * @param identifier the channel identifier to use
   * @param contents the message to send
   * @param consumer the consumer that will respond to the message
   */
  void sendLoginPluginMessage(ChannelIdentifier identifier, byte[] contents,
      MessageConsumer consumer);

  /**
   * Consumes the message.
   */
  interface MessageConsumer {

    /**
     * Consumes the message and responds to it.
     *
     * @param responseBody the message from the client, if any
     */
    void onMessageResponse(byte @Nullable [] responseBody);
  }
}
