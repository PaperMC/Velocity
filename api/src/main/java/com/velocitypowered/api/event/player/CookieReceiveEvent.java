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
import java.util.Arrays;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when a cookie response from a client is received by the proxy.
 * This usually happens after either a proxy plugin or a backend server requested a cookie.
 * Velocity will wait on this event to finish firing before discarding the
 * received cookie (if handled) or forwarding it to the backend server.
 */
@AwaitingEvent
public final class CookieReceiveEvent implements ResultedEvent<CookieReceiveEvent.ForwardResult> {

  private final Player player;
  private final Key originalKey;
  private final byte @Nullable [] originalData;
  private ForwardResult result;

  /**
   * Creates a new instance.
   *
   * @param player the player who sent the cookie response
   * @param key the identifier of the cookie
   * @param data the data of the cookie
   */
  public CookieReceiveEvent(final Player player, final Key key, final byte @Nullable [] data) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalKey = Preconditions.checkNotNull(key, "key");
    this.originalData = data;
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

  public byte @Nullable [] getOriginalData() {
    return originalData;
  }

  @Override
  public String toString() {
    return "CookieReceiveEvent{"
        + ", originalKey=" + originalKey
        + ", originalData=" + Arrays.toString(originalData)
        + ", result=" + result
        + '}';
  }

  /**
   * A result determining whether or not to forward the cookie response on.
   */
  public static final class ForwardResult implements ResultedEvent.Result {

    private static final ForwardResult ALLOWED = new ForwardResult(true, null, null);
    private static final ForwardResult DENIED = new ForwardResult(false, null, null);

    private final boolean status;
    private final Key key;
    private final byte[] data;

    private ForwardResult(final boolean status, final Key key, final byte[] data) {
      this.status = status;
      this.key = key;
      this.data = data;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    public Key getKey() {
      return key;
    }

    public byte[] getData() {
      return data;
    }

    @Override
    public String toString() {
      return status ? "forward to backend server" : "handled by proxy";
    }

    /**
     * Allows the cookie response to be forwarded to the backend server.
     *
     * @return the forward result
     */
    public static ForwardResult forward() {
      return ALLOWED;
    }

    /**
     * Prevents the cookie response from being forwarded to the backend server, the cookie response
     * is handled by the proxy.
     *
     * @return the handled result
     */
    public static ForwardResult handled() {
      return DENIED;
    }

    /**
     * Allows the cookie response to be forwarded to the backend server, but silently replaces the
     * identifier of the cookie with another.
     *
     * @param key the identifier to use instead
     * @return a result with a new key
     */
    public static ForwardResult key(final Key key) {
      Preconditions.checkNotNull(key, "key");
      return new ForwardResult(true, key, null);
    }

    /**
     * Allows the cookie response to be forwarded to the backend server, but silently replaces the
     * data of the cookie with another.
     *
     * @param data the data of the cookie to use instead
     * @return a result with new data
     */
    public static ForwardResult data(final byte[] data) {
      return new ForwardResult(true, null, data);
    }
  }
}
