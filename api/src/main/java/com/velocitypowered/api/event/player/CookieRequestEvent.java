/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;

/**
 * This event is fired when a cookie from a client is requested either by a proxy plugin or
 * by a backend server. Velocity will wait on this event to finish firing before discarding the
 * cookie request (if handled) or forwarding it to the client.
 */
@AwaitingEvent
public final class CookieRequestEvent implements ResultedEvent<CookieRequestEvent.ForwardResult> {

  private final Player player;
  private final Key originalKey;
  private ForwardResult result;

  /**
   * Creates a new instance.
   *
   * @param player the player from whom the cookies is requested
   * @param key the identifier of the cookie
   */
  public CookieRequestEvent(final Player player, final Key key) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalKey = Preconditions.checkNotNull(key, "key");
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

  public Player getPlayer() {
    return player;
  }

  public Key getOriginalKey() {
    return originalKey;
  }

  @Override
  public String toString() {
    return "CookieRequestEvent{"
        + ", originalKey=" + originalKey
        + ", result=" + result
        + '}';
  }

  /**
   * A result determining whether or not to forward the cookie request on.
   */
  public static final class ForwardResult implements Result {

    private static final ForwardResult ALLOWED = new ForwardResult(true, null);
    private static final ForwardResult DENIED = new ForwardResult(false, null);

    private final boolean status;
    private final Key key;

    private ForwardResult(final boolean status, final Key key) {
      this.status = status;
      this.key = key;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    public Key getKey() {
      return key;
    }

    @Override
    public String toString() {
      return status ? "forward to client" : "handled by proxy";
    }

    /**
     * Allows the cookie request to be forwarded to the client.
     *
     * @return the forward result
     */
    public static ForwardResult forward() {
      return ALLOWED;
    }

    /**
     * Prevents the cookie request from being forwarded to the client, the cookie request is
     * handled by the proxy.
     *
     * @return the handled result
     */
    public static ForwardResult handled() {
      return DENIED;
    }

    /**
     * Allows the cookie request to be forwarded to the client, but silently replaces the
     * identifier of the cookie with another.
     *
     * @param key the identifier to use instead
     * @return a result with a new key
     */
    public static ForwardResult key(final Key key) {
      Preconditions.checkNotNull(key, "key");
      return new ForwardResult(true, key);
    }
  }
}
