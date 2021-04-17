/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * Allows plugins to modify the packet indicating commands available on the server to a
 * Minecraft 1.13+ client.
 */
@Beta
public class PlayerAvailableCommandsEventImpl implements PlayerAvailableCommandsEvent {

  private final Player player;
  private final RootCommandNode<?> rootNode;

  /**
   * Constructs an available commands event.
   * @param player the targeted player
   * @param rootNode the Brigadier root node
   */
  public PlayerAvailableCommandsEventImpl(Player player,
      RootCommandNode<?> rootNode) {
    this.player = checkNotNull(player, "player");
    this.rootNode = checkNotNull(rootNode, "rootNode");
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public RootCommandNode<?> rootNode() {
    return rootNode;
  }
}
