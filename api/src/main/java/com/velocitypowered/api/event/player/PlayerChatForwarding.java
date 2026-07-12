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
import net.kyori.adventure.text.Component;

/**
 * Describes a proxy-controlled player-chat emission based on the original client-submitted
 * {@link PlayerChatMessage}.
 *
 * <p>The decorated content is sent as server-controlled chat decoration around the unchanged
 * signed body. The original body, signature, timestamp, salt, sender identity, and session
 * metadata remain part of the player-chat packet. This is not a formatting policy API; plugins
 * decide the decoration and recipients.</p>
 *
 * <p>This result is separate from a plaintext {@link PlayerChatEvent.ChatResult#message(String)}
 * rewrite. It does not mutate the original message and does not imply that Velocity has validated
 * the signature or message chain.</p>
 *
 * @since 3.6.0
 */
public final class PlayerChatForwarding {

  private final Component decoratedMessage;
  private final List<Player> recipients;

  /**
   * Creates a forwarding request.
   *
   * @param decoratedMessage the full decorated message clients should render
   * @param recipients the intended Velocity network recipients
   */
  public PlayerChatForwarding(Component decoratedMessage, Collection<? extends Player> recipients) {
    this.decoratedMessage = Preconditions.checkNotNull(decoratedMessage, "decoratedMessage");
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
   * Returns the intended network recipients.
   *
   * @return immutable recipient list
   */
  public List<Player> getRecipients() {
    return recipients;
  }
}
