package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * This event is fired when a handshake is established between a client and the proxy.
 */
public final class ConnectionHandshakeEvent implements ResultedEvent<ConnectionHandshakeEvent.ConnectionHandshakeComponentResult> {

  private final InboundConnection connection;
  private ConnectionHandshakeComponentResult result;
  private String newSocketAddressHostname;
  private String hostname;

  @Deprecated
  public ConnectionHandshakeEvent(InboundConnection connection) {
    this(connection, "127.0.0.1");
  }

  public ConnectionHandshakeEvent(InboundConnection connection, @NonNull String hostname) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.hostname = Preconditions.checkNotNull(hostname, "serverHostname");
    this.result = ConnectionHandshakeComponentResult.allowed();
  }

  public InboundConnection getConnection() {
    return connection;
  }

  /**
   * Sets the new handshake hostname. Keep in mind we will run a cleanup of the specified hostname
   * after it (removing SRV record points, removing everything after {@code \0}, ...)
   *
   * @param hostname the new handshake hostname
   * @see #getHostname()
   */
  public void setHostname(@NonNull String hostname) {
    this.hostname = hostname;
  }

  /**
   * Returns the handshake hostname.
   *
   * @return the handshake hostname
   * @see #setHostname(String)
   */
  @NonNull
  public String getHostname() {
    return hostname;
  }

  /**
   * Sets the socket address hostname (without the port) of the player.
   *
   * <p>This essentially changes the IP to the one specified.
   *
   * <p>We expect you to provide a *resolved* IP address!
   *
   * @param newSocketAddressHostname the new hostname of the socket address
   * @see #getNewSocketAddressHostname()
   */
  public void setNewSocketAddressHostname(String newSocketAddressHostname) {
    this.newSocketAddressHostname = newSocketAddressHostname;
  }

  /**
   * Returns the set hostname of the player's socket address.
   *
   * <p>This returns {@code null} if the socket address wasn't previously set by {@link
   * #setNewSocketAddressHostname(String)}.
   *
   * @return the hostname of the set socket address
   * @see #setNewSocketAddressHostname(String)
   */
  @Nullable
  public String getNewSocketAddressHostname() {
    return newSocketAddressHostname;
  }

  /**
   * Returns the result of the event.
   *
   * @return the result
   * @see #setResult(ConnectionHandshakeComponentResult)
   */
  @Override
  public ConnectionHandshakeComponentResult getResult() {
    return result;
  }

  /**
   * Sets the result of the event. If the {@code result} is denied, the player will disconnect with
   * the specified reason.
   *
   * <p>If the specified reason is {@code null}, the socket will be closed without a disconnect
   * message.
   *
   * @param result the new result
   * @see #getResult()
   */
  @Override
  public void setResult(ConnectionHandshakeComponentResult result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return "ConnectionHandshakeEvent{"
        + "connection=" + connection
        + ", result=" + result
        + ", newSocketAddressHostname='" + newSocketAddressHostname + '\''
        + ", hostname='" + hostname + '\''
        + '}';
  }

  /**
   * @implNote We don't use {@link com.velocitypowered.api.event.ResultedEvent.ComponentResult} as
   *     it doesn't allow for a {@code null} component result on deny.
   */
  public static final class ConnectionHandshakeComponentResult implements ResultedEvent.Result {

    private static final ConnectionHandshakeComponentResult ALLOWED = new ConnectionHandshakeComponentResult(true, null);

    private final boolean status;
    private final @Nullable Component reason;

    private ConnectionHandshakeComponentResult(boolean status, @Nullable Component reason) {
      this.status = status;
      this.reason = reason;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    public Optional<Component> getReason() {
      return Optional.ofNullable(reason);
    }

    @Override
    public String toString() {
      if (status) {
        return "allowed";
      }
      if (reason != null) {
        return "denied: " + PlainComponentSerializer.plain().serialize(reason);
      }
      return "denied";
    }

    /**
     * Returns a result indicating the connection will be allowed.
     *
     * @return the allowed result
     */
    public static ConnectionHandshakeComponentResult allowed() {
      return ALLOWED;
    }

    /**
     * Returns a result indicating the connection will be denied with no kick reason provided.
     *
     * @return the denied result
     * @see #denied(Component)
     */
    public static ConnectionHandshakeComponentResult denied() {
      return denied(null);
    }

    /**
     * Returns a result indicating the connection will be denied with the specified kick reason.
     *
     * @param reason the reason for disallowing the connection, can be {@code null}
     * @return the denied result
     * @see #denied()
     */
    public static ConnectionHandshakeComponentResult denied(@Nullable Component reason) {
      return new ConnectionHandshakeComponentResult(false, reason);
    }
  }
}
