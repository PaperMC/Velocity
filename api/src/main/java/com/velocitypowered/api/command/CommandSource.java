/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

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

  @Override
  default @NotNull Pointers pointers() {
    return Pointers.builder().withStatic(PermissionChecker.POINTER, getPermissionChecker()).build();
  }

  /**
   * Gets the permission checker for the invoker.
   *
   * @return invoker's permission checker
   */
  default PermissionChecker getPermissionChecker() {
    return permission -> {
      final Tristate state = getPermissionValue(permission);
      if (state == Tristate.TRUE) {
        return TriState.TRUE;
      } else if (state == Tristate.UNDEFINED) {
        return TriState.NOT_SET;
      } else if (state == Tristate.FALSE) {
        return TriState.FALSE;
      } else {
        throw new IllegalArgumentException();
      }
    };
  }
}
