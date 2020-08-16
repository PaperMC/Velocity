package com.velocitypowered.api.proxy;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Indicates an {@link Audience} that is on the proxy. This interface contains no-op default methods
 * that are used to bridge compatibility issues with the new adventure API. This interface will go
 * away in Velocity 2.0.0.
 *
 * @deprecated Only used to handle compatibility problems, will go away in Velocity 2.0.0
 */
@Deprecated
public interface ProxyAudience extends Audience {

  @Override
  void sendMessage(@NonNull Component message);

  @Override
  default void sendMessage(@NonNull Component message, @NonNull MessageType type) {
    sendMessage(message);
  }
}
