/*
 * Copyright (C) 2022-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;

/**
 * This event is fired when the downstream server tries to send a player a ResourcePack packet.
 * The proxy will wait on this event to finish before forwarding the resource pack to the user.
 * If this event is denied, it will retroactively send a DENIED status to the downstream
 * server in response.
 * If the downstream server has it set to "forced" it will forcefully disconnect the user.
 */
@AwaitingEvent
public class ServerResourcePackSendEvent implements ResultedEvent<ResultedEvent.GenericResult> {
  private GenericResult result;
  private final ResourcePackInfo receivedResourcePack;
  private ResourcePackInfo providedResourcePack;
  private final ServerConnection serverConnection;

  /**
   * Constructs a new ServerResourcePackSendEvent.
   *
   * @param receivedResourcePack The resource pack the server sent.
   * @param serverConnection The connection this occurred on.
   */
  public ServerResourcePackSendEvent(
      ResourcePackInfo receivedResourcePack,
      ServerConnection serverConnection
  ) {
    this.result = ResultedEvent.GenericResult.allowed();
    this.receivedResourcePack = receivedResourcePack;
    this.serverConnection = serverConnection;
    this.providedResourcePack = receivedResourcePack;
  }

  public ServerConnection getServerConnection() {
    return serverConnection;
  }

  public ResourcePackInfo getReceivedResourcePack() {
    return receivedResourcePack;
  }

  public ResourcePackInfo getProvidedResourcePack() {
    return providedResourcePack;
  }

  public void setProvidedResourcePack(ResourcePackInfo providedResourcePack) {
    this.providedResourcePack = providedResourcePack;
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
