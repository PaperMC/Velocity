/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * This event is fired when a plugin message is sent to the proxy, either from a client ({@link
 * Player}) or a server ({@link ServerConnection}). Velocity will wait on this event to finish
 * firing before discarding the sent plugin message (if handled) or forwarding it to the server.
 */
@AwaitingEvent
public final class PluginMessageEvent implements ResultedEvent<PluginMessageEvent.ForwardResult> {

  private final ChannelMessageSource source;
  private final ChannelMessageSink target;
  private final ChannelIdentifier identifier;
  private final byte[] data;
  private ForwardResult result;

  /**
   * Creates a new instance.
   *
   * @param source the source of the plugin message
   * @param target the destination of the plugin message
   * @param identifier the channel for this plugin message
   * @param data the payload of the plugin message
   */
  public PluginMessageEvent(ChannelMessageSource source, ChannelMessageSink target,
      ChannelIdentifier identifier, byte[] data) {
    this.source = Preconditions.checkNotNull(source, "source");
    this.target = Preconditions.checkNotNull(target, "target");
    this.identifier = Preconditions.checkNotNull(identifier, "identifier");
    this.data = Preconditions.checkNotNull(data, "data");
    this.result = ForwardResult.forward();
  }

  @Override
  public ForwardResult getResult() {
    return result;
  }

  @Override
  public void setResult(ForwardResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  public ChannelMessageSource getSource() {
    return source;
  }

  public ChannelMessageSink getTarget() {
    return target;
  }

  public ChannelIdentifier getIdentifier() {
    return identifier;
  }

  public byte[] getData() {
    return Arrays.copyOf(data, data.length);
  }

  public ByteArrayInputStream dataAsInputStream() {
    return new ByteArrayInputStream(data);
  }

  public ByteArrayDataInput dataAsDataStream() {
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

  /**
   * A result determining whether or not to forward this message on.
   */
  public static final class ForwardResult implements ResultedEvent.Result {

    private static final ForwardResult ALLOWED = new ForwardResult(true);
    private static final ForwardResult DENIED = new ForwardResult(false);

    private final boolean status;

    private ForwardResult(boolean b) {
      this.status = b;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    @Override
    public String toString() {
      return status ? "forward to sink" : "handled message at proxy";
    }

    public static ForwardResult forward() {
      return ALLOWED;
    }

    public static ForwardResult handled() {
      return DENIED;
    }
  }
}
