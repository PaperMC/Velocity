/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import static java.util.Objects.requireNonNull;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import java.net.InetSocketAddress;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * This event is executed before sending a player to another host,
 * either by the backend server or by a plugin using
 * the {@link Player#transferToHost(InetSocketAddress)} method.
 */
@AwaitingEvent
@ApiStatus.Experimental
public final class PreTransferEvent implements ResultedEvent<PreTransferEvent.TransferResult> {
  private final InetSocketAddress originalAddress;
  private final Player player;
  private TransferResult result = TransferResult.ALLOWED;

  public PreTransferEvent(final Player player, final InetSocketAddress address) {
    this.player = requireNonNull(player);
    this.originalAddress = requireNonNull(address);
  }

  public Player player() {
    return this.player;
  }

  public InetSocketAddress originalAddress() {
    return this.originalAddress;
  }

  @Override
  public TransferResult getResult() {
    return this.result;
  }

  @Override
  public void setResult(final TransferResult result) {
    requireNonNull(result);
    this.result = result;
  }

  /**
   * Transfer Result of a player to another host.
   */
  public static class TransferResult implements ResultedEvent.Result {
    private static final TransferResult ALLOWED = new TransferResult(true, null);
    private static final TransferResult DENIED = new TransferResult(false, null);

    private final InetSocketAddress address;
    private final boolean allowed;

    private TransferResult(final boolean allowed, final InetSocketAddress address) {
      this.address = address;
      this.allowed = allowed;
    }

    public static TransferResult allowed() {
      return ALLOWED;
    }

    public static TransferResult denied() {
      return DENIED;
    }

    /**
     * Sets the result of transfer to a specific host.
     *
     * @param address the address specified
     * @return a new TransferResult
     */
    public static TransferResult transferTo(final InetSocketAddress address) {
      requireNonNull(address);

      return new TransferResult(true, address);
    }

    @Override
    public boolean isAllowed() {
      return this.allowed;
    }

    @Nullable
    public InetSocketAddress address() {
      return this.address;
    }
  }
}
