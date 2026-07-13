/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes a proxy-controlled player-chat emission based on the original client-submitted
 * {@link PlayerChatMessage}.
 *
 * <p>The decorated content is sent as server-controlled chat decoration around the unchanged
 * signed body. The original body, signature, timestamp, salt, sender identity, and session
 * metadata remain part of the player-chat packet. This is not a formatting policy API; plugins
 * decide the decoration and recipients.</p>
 *
 * <p>An empty recipient list is a valid routing decision. It means the message intentionally has no
 * recipients, so Velocity will suppress the normal backend forwarding path without sending a
 * packet.</p>
 *
 * <p>This result is separate from a plaintext {@link PlayerChatEvent.ChatResult#message(String)}
 * rewrite. It does not mutate the original message and does not imply that Velocity has validated
 * the signature or message chain.</p>
 *
 * @since 3.6.0
 */
public final class PlayerChatForwarding {

  public static final int DEFAULT_CHAT_TYPE_HOLDER_ID = 1;

  private final Component decoratedMessage;
  private final Component senderName;
  private final @Nullable Component targetName;
  private final int chatTypeHolderId;
  private final List<Player> recipients;

  /**
   * Creates a forwarding request.
   *
   * @param decoratedMessage the full decorated message clients should render
   * @param recipients the intended Velocity network recipients
   */
  public PlayerChatForwarding(Component decoratedMessage, Collection<? extends Player> recipients) {
    this(decoratedMessage, Component.empty(), null, DEFAULT_CHAT_TYPE_HOLDER_ID, recipients);
  }

  /**
   * Creates a forwarding request with explicit clientbound chat-type parameters.
   *
   * @param decoratedMessage the full decorated message clients should render
   * @param senderName the sender component bound to the clientbound chat type
   * @param targetName the optional target component bound to the clientbound chat type
   * @param chatTypeHolderId the chat-type holder id to bind in the clientbound packet
   * @param recipients the intended Velocity network recipients
   */
  public PlayerChatForwarding(Component decoratedMessage, Component senderName,
      @Nullable Component targetName, int chatTypeHolderId, Collection<? extends Player> recipients) {
    this.decoratedMessage = Preconditions.checkNotNull(decoratedMessage, "decoratedMessage");
    this.senderName = Preconditions.checkNotNull(senderName, "senderName");
    this.targetName = targetName;
    this.chatTypeHolderId = chatTypeHolderId;
    this.recipients = List.copyOf(Preconditions.checkNotNull(recipients, "recipients"));
  }

  /**
   * Returns the server-controlled decorated content to render.
   *
   * @return the decorated message
   */
  public Component getDecoratedMessage() {
    return decoratedMessage;
  }

  /**
   * Returns the sender component bound to the clientbound chat type.
   *
   * @return the sender component
   */
  public Component getSenderName() {
    return senderName;
  }

  /**
   * Returns the optional target component bound to the clientbound chat type.
   *
   * @return the target component, if present
   */
  public Optional<Component> getTargetName() {
    return Optional.ofNullable(targetName);
  }

  /**
   * Returns the chat-type holder id used for the clientbound packet.
   *
   * @return the chat-type holder id
   */
  public int getChatTypeHolderId() {
    return chatTypeHolderId;
  }

  /**
   * Returns the intended network recipients.
   *
   * @return immutable recipient list
   */
  public List<Player> getRecipients() {
    return recipients;
  }
}
