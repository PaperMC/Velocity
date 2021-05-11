/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * This event is fired when a plugin message is sent to the proxy, either from a client ({@link
 * Player}) or a server ({@link ServerConnection}).
 */
public final class PluginMessageEventImpl implements PluginMessageEvent {

  private final ChannelMessageSource source;
  private final ChannelMessageSink target;
  private final PluginChannelId identifier;
  private final byte[] data;
  private GenericResult result;

  /**
   * Creates a new instance.
   *
   * @param source the source of the plugin message
   * @param target the destination of the plugin message
   * @param identifier the channel for this plugin message
   * @param data the payload of the plugin message
   */
  public PluginMessageEventImpl(ChannelMessageSource source, ChannelMessageSink target,
      PluginChannelId identifier, byte[] data) {
    this.source = Preconditions.checkNotNull(source, "source");
    this.target = Preconditions.checkNotNull(target, "target");
    this.identifier = Preconditions.checkNotNull(identifier, "identifier");
    this.data = Preconditions.checkNotNull(data, "data");
    this.result = GenericResult.allowed();
  }

  @Override
  public GenericResult result() {
    return result;
  }

  @Override
  public void setResult(GenericResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public ChannelMessageSource source() {
    return source;
  }

  @Override
  public ChannelMessageSink sink() {
    return target;
  }

  @Override
  public PluginChannelId channel() {
    return identifier;
  }

  @Override
  public byte[] rawMessage() {
    return Arrays.copyOf(data, data.length);
  }

  @Override
  public ByteArrayInputStream messageAsInputStream() {
    return new ByteArrayInputStream(data);
  }

  @Override
  public ByteArrayDataInput messageAsDataInput() {
    return ByteStreams.newDataInput(data);
  }

  @Override
  public String toString() {
    return "PluginMessageEvent{"
        + "source=" + source
        + ", target=" + target
        + ", identifier=" + identifier
        + ", data=" + Arrays.toString(data)
        + ", result=" + result
        + '}';
  }

}
