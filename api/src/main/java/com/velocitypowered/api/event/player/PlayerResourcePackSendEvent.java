/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when the downstream server tries to send a player a ResourcePackSend packet.
 * Velocity will wait on this event to finish before forwarding the packet to the user. If this
 * event is denied, it will retroactively send a DENIED status to the downstream server in response.
 * If the downstream server has it set to "forced" it will forcefully disconnect the user.
 */
public class PlayerResourcePackSendEvent implements ResultedEvent<ResultedEvent.GenericResult> {
  private GenericResult result;
  private String url;
  private @Nullable String hash;
  private boolean shouldForce;
  private @Nullable Component promptMessage;
  private boolean hasPromptMessage;
  private ServerInfo serverInfo;

  /**
   * Constructs a new PlayerResourcePackSendEvent.
   * 
   * @param url The url of the resource pack.
   * @param hash The hash of the resource pack.
   * @param shouldForce If the resource pack should force the user to accept.
   * @param promptMessage The message for the prompt if there is one. (1.17+)
   * @param hasPromptMessage Whether the resource pack should have a prompt or not.
   * @param serverInfo The server info of the server which sent the packet.
   */
  public PlayerResourcePackSendEvent(
      String url,
      @Nullable String hash,
      boolean shouldForce,
      @Nullable Component promptMessage,
      boolean hasPromptMessage,
      ServerInfo serverInfo
  ) {
    this.url = url;
    this.hash = hash;
    this.shouldForce = shouldForce;
    this.promptMessage = promptMessage;
    this.hasPromptMessage = hasPromptMessage;
    this.serverInfo = serverInfo;
  }

  public String url() {
    return this.url;
  }

  public void url(String url) {
    this.url = url;
  }

  public @Nullable String hash() {
    return this.hash;
  }

  public void hash(@Nullable String hash) {
    this.hash = hash;
  }

  public boolean shouldForce() {
    return this.shouldForce;
  }

  public void shouldForce(boolean shouldForce) {
    this.shouldForce = shouldForce;
  }

  public @Nullable Component promptMessage() {
    return this.promptMessage;
  }

  public void promptMessage(@Nullable Component promptMessage) {
    this.hasPromptMessage = promptMessage != null;
    this.promptMessage = promptMessage;
  }

  public boolean hasPromptMessage() {
    return this.hasPromptMessage;
  }

  public ServerInfo serverInfo() {
    return this.serverInfo;
  }

  @Override
  public GenericResult getResult() {
    return this.result;
  }

  @Override
  public void setResult(GenericResult result) {
    this.result = result;
  }
}
