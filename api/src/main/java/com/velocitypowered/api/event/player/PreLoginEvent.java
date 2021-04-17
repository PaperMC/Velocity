/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy
 * authenticates the player with Mojang or before the player's proxy connection is fully established
 * (for offline mode).
 */
public interface PreLoginEvent extends ResultedEvent<PreLoginEvent.PreLoginComponentResult> {

  InboundConnection connection();

  String username();

  /**
   * Represents an "allowed/allowed with forced online\offline mode/denied" result with a reason
   * allowed for denial.
   */
  final class PreLoginComponentResult implements Result {

    private static final PreLoginComponentResult ALLOWED = new PreLoginComponentResult(
        Result.ALLOWED, null);
    private static final PreLoginComponentResult FORCE_ONLINEMODE = new PreLoginComponentResult(
        Result.FORCE_ONLINE, null);
    private static final PreLoginComponentResult FORCE_OFFLINEMODE = new PreLoginComponentResult(
        Result.FORCE_OFFLINE, null);

    private final Result result;
    private final Component reason;

    private PreLoginComponentResult(Result result,
        @Nullable Component reason) {
      this.result = result;
      this.reason = reason;
    }

    @Override
    public boolean isAllowed() {
      return result != Result.DISALLOWED;
    }

    public Optional<Component> denialReason() {
      return Optional.ofNullable(reason);
    }

    public boolean isOnlineModeAllowed() {
      return result == Result.FORCE_ONLINE;
    }

    public boolean isForceOfflineMode() {
      return result == Result.FORCE_OFFLINE;
    }

    @Override
    public String toString() {
      switch (result) {
        case ALLOWED:
          return "allowed";
        case FORCE_OFFLINE:
          return "allowed with force offline mode";
        case FORCE_ONLINE:
          return "allowed with online mode";
        default:
          return "denied";
      }
    }

    /**
     * Returns a result indicating the connection will be allowed through the proxy.
     *
     * @return the allowed result
     */
    public static PreLoginComponentResult allowed() {
      return ALLOWED;
    }

    /**
     * Returns a result indicating the connection will be allowed through the proxy, but the
     * connection will be forced to use online mode provided that the proxy is in offline mode. This
     * acts similarly to {@link #allowed()} on an online-mode proxy.
     *
     * @return the result
     */
    public static PreLoginComponentResult forceOnlineMode() {
      return FORCE_ONLINEMODE;
    }

    /**
     * Returns a result indicating the connection will be allowed through the proxy, but the
     * connection will be forced to use offline mode even when the proxy is running in online mode.
     *
     * @return the result
     */
    public static PreLoginComponentResult forceOfflineMode() {
      return FORCE_OFFLINEMODE;
    }

    /**
     * Denies the login with the specified reason.
     *
     * @param reason the reason for disallowing the connection
     * @return a new result
     */
    public static PreLoginComponentResult denied(Component reason) {
      Preconditions.checkNotNull(reason, "reason");
      return new PreLoginComponentResult(Result.DISALLOWED, reason);
    }

    private enum Result {
      ALLOWED,
      FORCE_ONLINE,
      FORCE_OFFLINE,
      DISALLOWED
    }
  }
}
