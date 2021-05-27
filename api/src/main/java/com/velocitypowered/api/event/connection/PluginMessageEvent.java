/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import java.io.ByteArrayInputStream;

/**
 * This event is fired when a plugin message is sent to the proxy, either from a client ({@link
 * Player}) or a server ({@link ServerConnection}).
 */
public interface PluginMessageEvent extends ResultedEvent<ResultedEvent.GenericResult> {

  ChannelMessageSource source();

  ChannelMessageSink sink();

  PluginChannelId channel();

  byte[] rawMessage();

  ByteArrayInputStream messageAsInputStream();

  ByteArrayDataInput messageAsDataInput();

  default void setHandled(boolean handled) {
    if (handled) {
      setResult(ResultedEvent.GenericResult.denied());
    } else {
      setResult(ResultedEvent.GenericResult.allowed());
    }
  }
}
