package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.ProxyAudience;
import com.velocitypowered.api.util.AdventureCompat;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends PermissionSubject, ProxyAudience {

  /**
   * Sends the specified {@code component} to the invoker.
   *
   * @param component the text component to send
   * @deprecated Use {@link #sendMessage(Component)} instead
   */
  @Deprecated
  void sendMessage(net.kyori.text.Component component);

  @Override
  default void sendMessage(@NonNull Component message) {
    this.sendMessage(AdventureCompat.asOriginalTextComponent(message));
  }
}
