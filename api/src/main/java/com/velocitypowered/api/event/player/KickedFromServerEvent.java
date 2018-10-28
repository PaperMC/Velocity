package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Fired when a player is kicked from a server. You may either allow Velocity to kick the player
 * (with an optional reason override) or redirect the player to a separate server.
 */
public final class KickedFromServerEvent implements
    ResultedEvent<KickedFromServerEvent.ServerKickResult> {

  private final Player player;
  private final RegisteredServer server;
  private final Component originalReason;
  private final boolean duringServerConnect;
  private ServerKickResult result;

  public KickedFromServerEvent(Player player, RegisteredServer server, Component originalReason,
      boolean duringServerConnect, Component fancyReason) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.server = Preconditions.checkNotNull(server, "server");
    this.originalReason = Preconditions.checkNotNull(originalReason, "originalReason");
    this.duringServerConnect = duringServerConnect;
    this.result = new Notify(fancyReason);
  }

  @Override
  public ServerKickResult getResult() {
    return result;
  }

  @Override
  public void setResult(@NonNull ServerKickResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  public Player getPlayer() {
    return player;
  }

  public RegisteredServer getServer() {
    return server;
  }

  public Component getOriginalReason() {
    return originalReason;
  }

  /**
   * Returns whether or not the player got kicked while connecting to another server.
   *
   * @return whether or not the player got kicked
   */
  public boolean kickedDuringServerConnect() {
    return duringServerConnect;
  }

  /**
   * Returns whether or not the player got kicked while logging in.
   *
   * @return whether or not the player got kicked
   * @deprecated {@link #kickedDuringServerConnect()} has a better name and reflects the actual
   *     result
   */
  @Deprecated
  public boolean kickedDuringLogin() {
    return duringServerConnect;
  }

  /**
   * Represents the base interface for {@link KickedFromServerEvent} results.
   */
  public interface ServerKickResult extends ResultedEvent.Result {

  }

  /**
   * Tells the proxy to disconnect the player with the specified reason.
   */
  public static final class DisconnectPlayer implements ServerKickResult {

    private final Component component;

    private DisconnectPlayer(Component component) {
      this.component = Preconditions.checkNotNull(component, "component");
    }

    @Override
    public boolean isAllowed() {
      return true;
    }

    public Component getReason() {
      return component;
    }

    /**
     * Creates a new {@link DisconnectPlayer} with the specified reason.
     *
     * @param reason the reason to use when disconnecting the player
     * @return the disconnect result
     */
    public static DisconnectPlayer create(Component reason) {
      return new DisconnectPlayer(reason);
    }
  }

  /**
   * Tells the proxy to redirect the player to another server. No messages will be sent from the
   * proxy when this result is used.
   */
  public static final class RedirectPlayer implements ServerKickResult {

    private final RegisteredServer server;

    private RedirectPlayer(RegisteredServer server) {
      this.server = Preconditions.checkNotNull(server, "server");
    }

    @Override
    public boolean isAllowed() {
      return false;
    }

    public RegisteredServer getServer() {
      return server;
    }

    /**
     * Creates a new redirect result to forward the player to the specified {@code server}.
     *
     * @param server the server to send the player to
     * @return the redirect result
     */
    public static RedirectPlayer create(RegisteredServer server) {
      return new RedirectPlayer(server);
    }
  }

  /**
   * Notifies the player with the specified message but does nothing else. This is only a valid
   * result to use if the player was  trying to connect to a different server, otherwise it is
   * treated like a {@link DisconnectPlayer} result.
   */
  public static final class Notify implements ServerKickResult {

    private final Component message;

    private Notify(Component message) {
      this.message = Preconditions.checkNotNull(message, "message");
    }

    @Override
    public boolean isAllowed() {
      return false;
    }

    public Component getMessage() {
      return message;
    }

    /**
     * Notifies the player with the specified message but does nothing else.
     *
     * @param message the server to send the player to
     * @return the redirect result
     */
    public static Notify create(Component message) {
      return new Notify(message);
    }
  }
}
