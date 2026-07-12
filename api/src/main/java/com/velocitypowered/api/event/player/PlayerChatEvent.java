/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player types in a chat message. Velocity will wait on this event
 * to finish firing before forwarding it to the server, if the result allows it.
 */
@AwaitingEvent
public final class PlayerChatEvent implements ResultedEvent<PlayerChatEvent.ChatResult> {

  private final Player player;
  private final String message;
  private final MessageInfo messageInfo;
  private ChatResult result;

  /**
   * Constructs a PlayerChatEvent.
   *
   * @param player the player sending the message
   * @param message the message being sent
   */
  public PlayerChatEvent(Player player, String message) {
    this(player, message, MessageInfo.unsigned());
  }

  /**
   * Constructs a PlayerChatEvent.
   *
   * @param player the player sending the message
   * @param message the compatibility plaintext view of the message being sent
   * @param messageInfo information about the original client-submitted message
   * @since 3.6.0
   */
  public PlayerChatEvent(Player player, String message, MessageInfo messageInfo) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.message = Preconditions.checkNotNull(message, "message");
    this.messageInfo = Preconditions.checkNotNull(messageInfo, "messageInfo");
    this.result = ChatResult.allowed();
  }

  public Player getPlayer() {
    return player;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Returns information about the original Minecraft chat message submitted by the client.
   *
   * <p>The returned metadata describes the original protocol message. It is not affected by
   * later plugin changes to the {@link ChatResult}. {@link #getMessage()} remains the
   * compatibility plaintext view used by existing plugins.</p>
   *
   * <p>A signed message may be absent for unsigned or legacy chat. Its presence does not make
   * cancelling or rewriting signed chat protocol-safe; the existing modern Minecraft signed-chat
   * restrictions still apply.</p>
   *
   * @return original message information
   * @since 3.6.0
   */
  public MessageInfo getMessageInfo() {
    return messageInfo;
  }

  @Override
  public ChatResult getResult() {
    return result;
  }

  /**
   * Set result for the event.
   *
   * @param result the result of event
   * @deprecated for 1.19.1 and newer, set this as denied will kick users
   */
  @Deprecated
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
     *
     * @return the allowed result
     */
    public static ChatResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the message from being sent.
     *
     * @return the denied result
     */
    public static ChatResult denied() {
      return DENIED;
    }

    /**
     * Allows the message to be sent, but silently replaces it with another.
     *
     * @param message the message to use instead
     * @return a result with a new message
     */
    public static ChatResult message(@NonNull String message) {
      Preconditions.checkNotNull(message, "message");
      return new ChatResult(true, message);
    }
  }

  /**
   * Describes the signing state and original signed body of a player chat message.
   *
   * <p>This object represents the message as submitted by the Minecraft client, before any plugin
   * result can deny or replace the forwarded text.</p>
   *
   * @since 3.6.0
   */
  public static final class MessageInfo {

    private static final MessageInfo SIGNED = new MessageInfo(SignedState.SIGNED, null);
    private static final MessageInfo UNSIGNED = new MessageInfo(SignedState.UNSIGNED, null);
    private static final MessageInfo LEGACY = new MessageInfo(SignedState.LEGACY, null);

    private final SignedState signedState;
    private final @Nullable SignedMessage signedMessage;

    private MessageInfo(SignedState signedState, @Nullable SignedMessage signedMessage) {
      this.signedState = Preconditions.checkNotNull(signedState, "signedState");
      this.signedMessage = signedMessage;
    }

    /**
     * Returns the signing state of the original client-submitted chat message.
     *
     * @return the signed state
     * @since 3.6.0
     */
    public SignedState getSignedState() {
      return signedState;
    }

    /**
     * Returns the original Minecraft signed message, if the client submitted one.
     *
     * <p>The signed message body is the original signed body and is separate from any later
     * {@link ChatResult} rewrite.</p>
     *
     * @return the original signed message, if present
     * @since 3.6.0
     */
    public Optional<SignedMessage> getSignedMessage() {
      return Optional.ofNullable(signedMessage);
    }

    /**
     * Creates message information for a signed Minecraft chat message.
     *
     * @param signedMessage the original client-submitted signed message
     * @return signed message information
     * @since 3.6.0
     */
    public static MessageInfo signed(SignedMessage signedMessage) {
      return new MessageInfo(SignedState.SIGNED, Preconditions.checkNotNull(signedMessage,
          "signedMessage"));
    }

    /**
     * Creates message information for a signed Minecraft chat message without a complete
     * public signed-message representation.
     *
     * @return signed message information
     * @since 3.6.0
     */
    public static MessageInfo signed() {
      return SIGNED;
    }

    /**
     * Creates message information for unsigned modern Minecraft chat.
     *
     * @return unsigned message information
     * @since 3.6.0
     */
    public static MessageInfo unsigned() {
      return UNSIGNED;
    }

    /**
     * Creates message information for legacy chat that has no modern signed-chat metadata.
     *
     * @return legacy message information
     * @since 3.6.0
     */
    public static MessageInfo legacy() {
      return LEGACY;
    }
  }

  /**
   * Represents the signing state of the original player chat message.
   *
   * @since 3.6.0
   */
  public enum SignedState {
    /**
     * The client submitted a modern Minecraft signed chat message.
     */
    SIGNED,
    /**
     * The client submitted modern Minecraft chat without a message signature.
     */
    UNSIGNED,
    /**
     * The protocol version does not provide modern signed-chat metadata.
     */
    LEGACY
  }
}
