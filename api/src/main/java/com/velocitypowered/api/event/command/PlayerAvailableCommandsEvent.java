/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * Allows plugins to modify the packet indicating commands available on the server to a
 * Minecraft 1.13+ client.
 */
public interface PlayerAvailableCommandsEvent extends Event {

  Player player();

  RootCommandNode<?> rootNode();
}
