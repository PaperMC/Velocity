/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends Audience, PermissionSubject {

  /**
   * Sends a message with the MiniMessage format to this source.
   *
   * @param message MiniMessage content
   * @see <a href="https://docs.papermc.io/adventure/minimessage/format/">MiniMessage docs</a>
   *      for more information on the format.
   */
  default void sendRichMessage(final @NotNull String message) {
    this.sendMessage(MiniMessage.miniMessage().deserialize(message, this));
  }

  /**
   * Sends a message with the MiniMessage format to this source.
   *
   * @param message MiniMessage content
   * @param resolvers resolvers to use
   * @see <a href="https://docs.papermc.io/adventure/minimessage/">MiniMessage docs</a>
   *     and <a href="https://docs.papermc.io/adventure/minimessage/dynamic-replacements">MiniMessage Placeholders docs</a>
   *     for more information on the format.
   */
  default void sendRichMessage(
          final @NotNull String message,
          final @NotNull TagResolver @NotNull... resolvers
  ) {
    this.sendMessage(MiniMessage.miniMessage().deserialize(message, this, resolvers));
  }

  /**
   * Sends a plain message to this source.
   *
   * @param message plain message
   * @apiNote This method will not apply any form of parse to the text provided,
   *      however, it is recommended not to use legacy color codes as this is a deprecated format
   *     and not recommended.
   */
  default void sendPlainMessage(final @NotNull String message) {
    this.sendMessage(Component.text(message));
  }
}
