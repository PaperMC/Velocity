/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends Audience, PermissionSubject {

  /**
   * Sends the specified {@code component} to the invoker.
   *
   * @param component the text component to send
   * @deprecated Use {@link #sendMessage(Identified, Component)}
   *     or {@link #sendMessage(Identity, Component)} instead
   */
  @Deprecated
  void sendMessage(net.kyori.text.Component component);

  @Override
  default void sendMessage(@NonNull Identity identity, @NonNull Component message,
                           @NonNull MessageType type) {
    this.sendMessage(LegacyText3ComponentSerializer.get().serialize(message));
  }
}
