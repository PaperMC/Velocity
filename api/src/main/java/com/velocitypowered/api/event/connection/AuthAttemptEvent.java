package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <p>This event is fired when a player attempts to authenticate with the proxy, regardless of whether
 * the connection is online-mode or offline-mode. The proxy implementation will provide a default
 * authentication mechanism (using the Mojang session server), but plugins can subscribe to this
 * event to implement custom authentication logic.</p>
 *
 * <p>If the connection is {@linkplain AuthAttemptEvent#isOnlineMode() online-mode}, the
 * {@linkplain AuthAttemptEvent#getSharedSecret() shared secret} will always be present, as encryption
 * is always enabled in online-mode. However, in offline-mode, encryption may or may not be enabled
 * (configurable in proxy config file), and thus the shared secret may be {@code null}.</p>
 *
 * <p>If the result of this event is {@code null}, online-mode connections will assume failure, but
 * offline-mode connections will assume success. The default authentication mechanism will not
 * provide a result for offline-mode connections.</p>
 *
 * <p>Note that a successful result from this event does not necessarily determine the player
 * profile, as plugins may override this using {@link GameProfileRequestEvent}.</p>
 */
public final class AuthAttemptEvent implements ResultedEvent<AuthAttemptEvent.AuthResult> {

  private final @NonNull InboundConnection connection;
  private final boolean onlineMode;
  private final byte @Nullable [] sharedSecret;
  private final @NonNull String attemptedUsername;

  private @Nullable AuthResult result;

  /**
   * Constructs a new authentication attempt event.
   *
   * @param connection the inbound connection attempting to authenticate
   * @param onlineMode whether the connection is in online-mode
   * @param sharedSecret the shared secret for online-mode connections, or {@code null} for offline-mode
   * @param attemptedUsername the username that the player is attempting to authenticate with
   */
  public AuthAttemptEvent(@NonNull InboundConnection connection, boolean onlineMode,
                          byte @Nullable [] sharedSecret, @NonNull String attemptedUsername) {
    this.connection = connection;
    this.onlineMode = onlineMode;
    this.sharedSecret = sharedSecret;
    this.attemptedUsername = attemptedUsername;
  }

  /**
   * Gets the inbound connection that is attempting to authenticate.
   *
   * @return the inbound connection
   */
  public @NonNull InboundConnection getConnection() {
    return connection;
  }

  /**
   * Gets if the connection is in online-mode.
   *
   * @return {@code true} if the connection is in online-mode, {@code false} otherwise
   */
  public boolean isOnlineMode() {
    return onlineMode;
  }

  /**
   * Gets the shared secret for online-mode connections, or {@code null} for offline-mode connections.
   *
   * @return the shared secret, or {@code null} if not applicable
   */
  public byte @Nullable [] getSharedSecret() {
    return sharedSecret;
  }

  /**
   * Gets the username that the player is attempting to authenticate with.
   *
   * @return the attempted username
   */
  public @NonNull String getAttemptedUsername() {
    return attemptedUsername;
  }

  /**
   * Gets the result of the authentication attempt.
   *
   * @return the authentication result, or {@code null} if not set
   */
  @Override
  public @Nullable AuthResult getResult() {
    return result;
  }

  /**
   * Sets the result of the authentication attempt.
   *
   * @param result the authentication result, or {@code null} to indicate no result
   */
  @Override
  public void setResult(@Nullable AuthResult result) {
    this.result = result;
  }

  /**
   * Interface representing the result of an authentication attempt.
   */
  public sealed interface AuthResult extends Result permits SuccessResult, FailureResult {
  }

  /**
   * Represents a successful authentication result, containing the player's profile.
   */
  public record SuccessResult(@NonNull GameProfile profile) implements AuthResult {
    @Override
    public boolean isAllowed() {
      return true;
    }
  }

  /**
   * Represents a failed authentication result, containing a reason for the failure.
   */
  public record FailureResult(@NonNull Component reason) implements AuthResult {
    @Override
    public boolean isAllowed() {
      return false;
    }
  }

}
