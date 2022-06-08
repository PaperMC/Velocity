/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Allows the server to communicate with a client logging into the proxy using login plugin
 * messages.
 */
public interface LoginPhaseConnection extends InboundConnection, KeyIdentifiable {
  void sendLoginPluginMessage(ChannelIdentifier identifier, byte[] contents,
      MessageConsumer consumer);

  interface MessageConsumer {
    void onMessageResponse(byte @Nullable [] responseBody);
  }
}
