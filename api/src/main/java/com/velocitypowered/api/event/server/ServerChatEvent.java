package com.velocitypowered.api.event.server;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.server.ServerChatEvent.ServerChatResult;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a server types in a message. Example of server "chatting": join and quit
 * messages. Event usage example: <i>manipulate server messages and generate them by proxy
 * instead</i>.
 */
public final class ServerChatEvent implements ResultedEvent<ServerChatResult> {

  private ServerChatResult result;
  private final RegisteredServer server;
  private final String message;

  /**
   * Constructs a ServerChatEvent.
   *
   * @param server the server sending the message
   * @param message the message being sent
   */
  public ServerChatEvent(RegisteredServer server, String message) {
    this.server = server;
    this.message = message;
  }

  public RegisteredServer getServer() {
    return server;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public ServerChatResult getResult() {
    return result;
  }

  @Override
  public void setResult(ServerChatResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  /**
   * Represents the result of the {@link ServerChatEvent}.
   */
  public static final class ServerChatResult implements ResultedEvent.Result {

    private static final ServerChatResult ALLOWED = new ServerChatResult(true, null);
    private static final ServerChatResult DENIED = new ServerChatResult(false, null);

    private @Nullable final String message;
    private final boolean status;

    private ServerChatResult(boolean status, @Nullable String message) {
      this.status = status;
      this.message = message;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    /**
     * Allows the message to be sent, without modification.
     *
     * @return the allowed result
     */
    public static ServerChatResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the message from being sent.
     *
     * @return the denied result
     */
    public static ServerChatResult denied() {
      return DENIED;
    }

    public Optional<String> getMessage() {
      return Optional.ofNullable(message);
    }

    /**
     * Allows the message to be sent, but silently replaced with another.
     *
     * @param message the message to use instead
     * @return a result with a new message
     */
    public static ServerChatResult message(String message) {
      return new ServerChatResult(true, message);
    }

    @Override
    public String toString() {
      return status ? "allowed" : "denied";
    }
  }
}
