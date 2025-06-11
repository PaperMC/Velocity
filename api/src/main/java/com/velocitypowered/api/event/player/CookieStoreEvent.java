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

/**
 * This event is fired when a cookie should be stored on a player's client. This process can be
 * initiated either by a proxy plugin or by a backend server. Velocity will wait on this event
 * to finish firing before discarding the cookie (if handled) or forwarding it to the client so
 * that it can store the cookie.
 */
@AwaitingEvent
public final class CookieStoreEvent implements ResultedEvent<CookieStoreEvent.ForwardResult> {

  private final Player player;
  private final Key originalKey;
  private final byte[] originalData;
  private ForwardResult result;

  /**
   * Creates a new instance.
   *
   * @param player the player who should store the cookie
   * @param key the identifier of the cookie
   * @param data the data of the cookie
   */
  public CookieStoreEvent(final Player player, final Key key, final byte[] data) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalKey = Preconditions.checkNotNull(key, "key");
    this.originalData = Preconditions.checkNotNull(data, "data");
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

  public byte[] getOriginalData() {
    return originalData;
  }

  @Override
  public String toString() {
    return "CookieStoreEvent{"
        + ", originalKey=" + originalKey
        + ", originalData=" + Arrays.toString(originalData)
        + ", result=" + result
        + '}';
  }

  /**
   * A result determining whether or not to forward the cookie on.
   */
  public static final class ForwardResult implements Result {

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
      return status ? "forward to client" : "handled by proxy";
    }

    /**
     * Allows the cookie to be forwarded to the client so that it can store it.
     *
     * @return the forward result
     */
    public static ForwardResult forward() {
      return ALLOWED;
    }

    /**
     * Prevents the cookie from being forwarded to the client, the cookie is handled by the proxy.
     *
     * @return the handled result
     */
    public static ForwardResult handled() {
      return DENIED;
    }

    /**
     * Allows the cookie to be forwarded to the client so that it can store it, but silently
     * replaces the identifier of the cookie with another.
     *
     * @param key the identifier to use instead
     * @return a result with a new key
     */
    public static ForwardResult key(final Key key) {
      Preconditions.checkNotNull(key, "key");
      return new ForwardResult(true, key, null);
    }

    /**
     * Allows the cookie to be forwarded to the client so that it can store it, but silently
     * replaces the data of the cookie with another.
     *
     * @param data the data of the cookie to use instead
     * @return a result with new data
     */
    public static ForwardResult data(final byte[] data) {
      Preconditions.checkNotNull(data, "data");
      return new ForwardResult(true, null, data);
    }
  }
}
