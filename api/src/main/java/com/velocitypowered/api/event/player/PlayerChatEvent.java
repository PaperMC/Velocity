/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player types in a chat message.
 */
public final class PlayerChatEvent implements ResultedEvent<PlayerChatEvent.ChatResult> {

  private final Player player;
  private final String message;
  private ChatResult result;

  /**
   * Constructs a PlayerChatEvent.
   * @param player the player sending the message
   * @param message the message being sent
   */
  public PlayerChatEvent(Player player, String message) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.message = Preconditions.checkNotNull(message, "message");
    this.result = ChatResult.allowed();
  }

  public Player getPlayer() {
    return player;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public ChatResult getResult() {
    return result;
  }

  @Override
  public void setResult(ChatResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "PlayerChatEvent{"
        + "player=" + player
        + ", message=" + message
        + ", result=" + result
        + '}';
  }

  /**
   * Represents the result of the {@link PlayerChatEvent}.
   */
  public static final class ChatResult implements ResultedEvent.Result {

    private static final ChatResult ALLOWED = new ChatResult(true, null);
    private static final ChatResult DENIED = new ChatResult(false, null);

    private @Nullable String message;
    private final boolean status;

    private ChatResult(boolean status, @Nullable String message) {
      this.status = status;
      this.message = message;
    }

    public Optional<String> getMessage() {
      return Optional.ofNullable(message);
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    @Override
    public String toString() {
      return status ? "allowed" : "denied";
    }

    /**
     * Allows the message to be sent, without modification.
     * @return the allowed result
     */
    public static ChatResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the message from being sent.
     * @return the denied result
     */
    public static ChatResult denied() {
      return DENIED;
    }

    /**
     * Allows the message to be sent, but silently replaced with another.
     * @param message the message to use instead
     * @return a result with a new message
     */
    public static ChatResult message(@NonNull String message) {
      Preconditions.checkNotNull(message, "message");
      return new ChatResult(true, message);
    }
  }
}
