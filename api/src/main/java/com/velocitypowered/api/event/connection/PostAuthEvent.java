/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired after Mojang authentication response is received, before Velocity decides what to do
 * with offline (cracked) players. Online (verified) players are also passed through here
 * with {@code onlineMode = true}.
 */
@AwaitingEvent
public final class PostAuthEvent implements ResultedEvent<PostAuthEvent.PostAuthResult> {

  private final InboundConnection connection;
  private final String username;
  private final boolean onlineMode;
  private final @Nullable GameProfile onlineProfile;
  private PostAuthResult result;

  /**
   * Creates a new PostAuthEvent.
   *
   * @param connection    the inbound connection
   * @param username      the player's username
   * @param onlineMode    true if Mojang verified the account (HTTP 200), false if cracked (HTTP 204)
   * @param onlineProfile the Mojang game profile, only present when onlineMode is true
   */
  public PostAuthEvent(
      InboundConnection connection,
      String username,
      boolean onlineMode,
      @Nullable GameProfile onlineProfile
  ) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.username = Preconditions.checkNotNull(username, "username");
    this.onlineMode = onlineMode;
    this.onlineProfile = onlineProfile;
    this.result = onlineMode
        ? PostAuthResult.allowOnline()
        : PostAuthResult.denied(Component.translatable("velocity.error.online-mode-only"));
  }

  public InboundConnection getConnection() {
    return connection;
  }

  public String getUsername() {
    return username;
  }

  /**
   * Returns true if Mojang returned HTTP 200 (verified premium account),
   * false if HTTP 204 (cracked/offline player).
   *
   * @return whether the player is online mode
   */
  public boolean isOnlineMode() {
    return onlineMode;
  }

  /**
   * Returns the Mojang game profile, only present when {@link #isOnlineMode()} is true.
   *
   * @return the online game profile, if present
   */
  public Optional<GameProfile> getOnlineProfile() {
    return Optional.ofNullable(onlineProfile);
  }

  @Override
  public PostAuthResult getResult() {
    return result;
  }

  @Override
  public void setResult(final @NonNull PostAuthResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PostAuthEvent{"
        + "username='" + username + '\''
        + ", onlineMode=" + onlineMode
        + ", result=" + result
        + '}';
  }

  /**
   * Represents the result of a {@link PostAuthEvent}.
   */
  public static final class PostAuthResult implements ResultedEvent.Result {

    private final ResultType type;
    private final @Nullable Component reason;

    private PostAuthResult(ResultType type, @Nullable Component reason) {
      this.type = type;
      this.reason = reason;
    }

    @Override
    public boolean isAllowed() {
      return type != ResultType.DENIED;
    }

    /**
     * Returns true if the player should be allowed as a verified online player.
     *
     * @return whether online mode is allowed
     */
    public boolean isAllowOnline() {
      return type == ResultType.ALLOW_ONLINE;
    }

    /**
     * Returns true if the player should be allowed as an offline/cracked player.
     *
     * @return whether offline mode is allowed
     */
    public boolean isAllowOffline() {
      return type == ResultType.ALLOW_OFFLINE;
    }

    /**
     * Returns the denial reason, if present.
     *
     * @return the reason component
     */
    public Optional<Component> getReasonComponent() {
      return Optional.ofNullable(reason);
    }

    /**
     * Allow as a verified online player using their Mojang GameProfile.
     *
     * @return the allow-online result
     */
    public static PostAuthResult allowOnline() {
      return new PostAuthResult(ResultType.ALLOW_ONLINE, null);
    }

    /**
     * Allow as an offline/cracked player.
     * Velocity will generate an offline GameProfile for them.
     *
     * @return the allow-offline result
     */
    public static PostAuthResult allowOffline() {
      return new PostAuthResult(ResultType.ALLOW_OFFLINE, null);
    }

    /**
     * Deny the connection with the given reason.
     *
     * @param reason the kick reason
     * @return the denied result
     */
    public static PostAuthResult denied(Component reason) {
      Preconditions.checkNotNull(reason, "reason");
      return new PostAuthResult(ResultType.DENIED, reason);
    }

    /**
     * The type of result.
     */
    public enum ResultType {
      /** Allow as Mojang-verified online player. */
      ALLOW_ONLINE,
      /** Allow as offline/cracked player. */
      ALLOW_OFFLINE,
      /** Deny the connection. */
      DENIED
    }
  }
}