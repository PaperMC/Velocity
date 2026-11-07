/*
 * Copyright (C) 2026 Velocity Contributors
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
import net.kyori.adventure.nbt.BinaryTag;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a {@link Player} sends the custom click action packet to the proxy. Velocity will
 * wait on this event to finish firing before discarding the sent custom click action packet (if
 * handled) or forwarding it to the server.
 *
 * @since 4.2.1
 * @sinceMinecraft 1.21.6
 */
@AwaitingEvent
public class PlayerCustomClickEvent implements ResultedEvent<PlayerCustomClickEvent.ForwardResult> {

  private final Player player;
  private final Key id;
  private final BinaryTag payload;
  private ForwardResult result;

  /**
   * Constructs a new PlayerCustomClickEvent.
   *
   * @param player  the {@link Player} of the send custom click action packet
   * @param id      the send action id
   * @param payload the send action payload
   */
  public PlayerCustomClickEvent(@NonNull Player player, @NonNull Key id, @NonNull BinaryTag payload) {
    Preconditions.checkNotNull(player);
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(payload);
    this.player = player;
    this.id = id;
    this.payload = payload;
    this.result = ForwardResult.forward();
  }

  public @NonNull Player player() {
    return player;
  }

  public @NonNull Key id() {
    return id;
  }

  public @NonNull BinaryTag payload() {
    return payload;
  }

  @Override
  public ForwardResult getResult() {
    return result;
  }

  @Override
  public void setResult(ForwardResult result) {
    this.result = result;
  }

  @Override
  public @NonNull String toString() {
    return "PlayerCustomClickEvent{"
        + "player=" + player
        + ", id='" + id + "'"
        + ", payload='" + payload + "'"
        + ", result=" + result
        + "}";
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
      return status ? "forward to sink" : "handled click at proxy";
    }

    public static ForwardResult forward() {
      return ALLOWED;
    }

    public static ForwardResult handled() {
      return DENIED;
    }
  }
}

