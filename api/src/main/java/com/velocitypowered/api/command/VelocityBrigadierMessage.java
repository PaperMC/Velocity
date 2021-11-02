/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Represents an implementation of brigadier's {@link Message}, providing support for {@link
 * Component} messages.
 */
public final class VelocityBrigadierMessage implements Message {

  private final Component message;

  public VelocityBrigadierMessage(Component message) {
    this.message = Preconditions.checkNotNull(message, "message");
  }

  /**
   * Returns the message as a {@link Component}.
   *
   * @return message as component
   */
  public Component asComponent() {
    return message;
  }

  /**
   * Returns the message as a legacy text.
   *
   * @return message as legacy text
   */
  @Override
  public String getString() {
    return LegacyComponentSerializer.legacySection().serialize(message);
  }
}
