/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import com.velocitypowered.api.event.connection.PluginMessageEventImpl;

/**
 * Represents an interface to register and unregister {@link PluginChannelId}s for the proxy to
 * listen on.
 */
public interface ChannelRegistrar {

  /**
   * Registers the specified message identifiers to listen on so you can intercept plugin messages
   * on the channel using {@link PluginMessageEventImpl}.
   *
   * @param identifiers the channel identifiers to register
   */
  void register(PluginChannelId... identifiers);

  /**
   * Removes the intent to listen for the specified channel.
   *
   * @param identifiers the identifiers to unregister
   */
  void unregister(PluginChannelId... identifiers);
}
