package com.velocitypowered.proxy.connection.util;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import java.util.Optional;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;

public class ConnectionRequestResults {

  private ConnectionRequestResults() {
    throw new AssertionError();
  }

  public static Impl successful(RegisteredServer server) {
    return plainResult(Status.SUCCESS, server);
  }

  /**
   * Returns a plain result (one with a status but no reason).
   * @param status the status to use
   * @param server the server to use
   * @return the result
   */
  public static Impl plainResult(
      ConnectionRequestBuilder.Status status,
      RegisteredServer server) {
    return new Impl(status, null, server, true);
  }

  /**
   * Returns a disconnect result with a reason.
   * @param component the reason for disconnecting from the server
   * @param server the server to use
   * @return the result
   */
  public static Impl forDisconnect(Component component, RegisteredServer server) {
    return new Impl(Status.SERVER_DISCONNECTED, component, server, true);
  }

  public static Impl forDisconnect(Disconnect disconnect, RegisteredServer server) {
    Component deserialized = GsonComponentSerializer.INSTANCE.deserialize(disconnect.getReason());
    return forDisconnect(deserialized, server);
  }

  public static Impl forUnsafeDisconnect(Disconnect disconnect, RegisteredServer server) {
    Component deserialized = GsonComponentSerializer.INSTANCE.deserialize(disconnect.getReason());
    return new Impl(Status.SERVER_DISCONNECTED, deserialized, server, false);
  }

  public static class Impl implements ConnectionRequestBuilder.Result {

    private final Status status;
    private final @Nullable Component component;
    private final RegisteredServer attemptedConnection;
    private final boolean safe;

    Impl(Status status, @Nullable Component component,
        RegisteredServer attemptedConnection, boolean safe) {
      this.status = status;
      this.component = component;
      this.attemptedConnection = attemptedConnection;
      this.safe = safe;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public Optional<Component> getReason() {
      return Optional.ofNullable(component);
    }

    @Override
    public RegisteredServer getAttemptedConnection() {
      return attemptedConnection;
    }

    /**
     * Returns whether or not it is safe to attempt a reconnect.
     * @return whether we can try to reconnect
     */
    public boolean isSafe() {
      return safe;
    }
  }
}
