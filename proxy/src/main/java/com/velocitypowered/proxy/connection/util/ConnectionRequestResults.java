package com.velocitypowered.proxy.connection.util;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Result;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import java.util.Optional;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;

public class ConnectionRequestResults {

  private ConnectionRequestResults() {
    throw new AssertionError();
  }

  public static Result successful(RegisteredServer server) {
    return plainResult(Status.SUCCESS, server);
  }

  /**
   * Returns a plain result (one with a status but no reason).
   * @param status the status to use
   * @param server the server to use
   * @return the result
   */
  public static ConnectionRequestBuilder.Result plainResult(
      ConnectionRequestBuilder.Status status,
      RegisteredServer server) {
    return new ConnectionRequestBuilder.Result() {
      @Override
      public ConnectionRequestBuilder.Status getStatus() {
        return status;
      }

      @Override
      public Optional<Component> getReason() {
        return Optional.empty();
      }

      @Override
      public RegisteredServer getAttemptedConnection() {
        return server;
      }
    };
  }

  public static ConnectionRequestBuilder.Result forDisconnect(Disconnect disconnect,
      RegisteredServer server) {
    Component deserialized = ComponentSerializers.JSON.deserialize(disconnect.getReason());
    return forDisconnect(deserialized, server);
  }

  /**
   * Returns a disconnect result with a reason.
   * @param component the reason for disconnecting from the server
   * @param server the server to use
   * @return the result
   */
  public static ConnectionRequestBuilder.Result forDisconnect(Component component,
      RegisteredServer server) {
    return new ConnectionRequestBuilder.Result() {
      @Override
      public ConnectionRequestBuilder.Status getStatus() {
        return ConnectionRequestBuilder.Status.SERVER_DISCONNECTED;
      }

      @Override
      public Optional<Component> getReason() {
        return Optional.of(component);
      }

      @Override
      public RegisteredServer getAttemptedConnection() {
        return server;
      }
    };
  }
}
