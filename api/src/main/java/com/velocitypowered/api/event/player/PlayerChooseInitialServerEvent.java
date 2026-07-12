/*
 * Copyright (C) 2019-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a player has finished the login process, and we need to choose the first server
 * to connect to. Velocity will wait on this event to finish firing before initiating the connection
 * but you should try to limit the work done in this event. Failures will be handled by
 * {@link KickedFromServerEvent} as normal.
 */
@AwaitingEvent
public class PlayerChooseInitialServerEvent implements ResultedEvent<PlayerChooseInitialServerEvent.Result> {

  private final Player player;
  private @NotNull Result result;

  /**
   * Constructs a PlayerChooseInitialServerEvent.
   *
   * @param player        the player that was connected
   * @param initialServer the initial server selected, may be {@code null}
   */
  public PlayerChooseInitialServerEvent(Player player, @Nullable RegisteredServer initialServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.result = Allowed.of(initialServer);
  }

  public Player getPlayer() {
    return player;
  }

  public Optional<RegisteredServer> getInitialServer() {
    if (result instanceof Allowed allowed) {
      return Optional.ofNullable(allowed.initialServer());
    }
    return Optional.empty();
  }

  /**
   * Sets the new initial server.
   *
   * @param server the initial server the player should connect to
   */
  public void setInitialServer(@Nullable RegisteredServer server) {
    if (server == null) {
      this.result = Denied.of();
      return;
    }
    this.result = Allowed.of(server);
  }

  @Override
  public String toString() {
    return "PlayerChooseInitialServerEvent{"
      + "player=" + player
      + ", result=" + result
      + '}';
  }

  @Override
  public @NonNull Result getResult() {
    return this.result;
  }

  @Override
  public void setResult(final Result result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  public sealed interface Result extends ResultedEvent.Result permits Allowed, Denied {
  }

  public record Allowed(@NotNull RegisteredServer initialServer) implements Result {

    public Allowed {
      Preconditions.checkNotNull(initialServer, "initialServer");
    }

    public static @NotNull Result of(final RegisteredServer initialServer) {
      return new Allowed(initialServer);
    }

    @Override
    public boolean isAllowed() {
      return true;
    }
  }

  public record Denied(@Nullable Component reason) implements Result {
    public static @NotNull Result of(@Nullable Component reason) {
      return new Denied(reason);
    }

    public static @NotNull Result of() {
      return new Denied(null);
    }

    @Override
    public boolean isAllowed() {
      return false;
    }
  }
}
