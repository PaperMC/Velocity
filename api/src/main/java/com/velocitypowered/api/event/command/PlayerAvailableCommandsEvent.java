/*
 * Copyright (C) 2020-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;

/**
 * Allows plugins to modify the packet indicating commands available on the server to a
 * Minecraft 1.13+ client. The given {@link RootCommandNode} is mutable. Velocity will wait
 * for this event to finish firing before sending the list of available commands to the
 * client.
 */
@AwaitingEvent
@Beta
public class PlayerAvailableCommandsEvent {

  private final Player player;
  private final RootCommandNode<?> rootNode;

  /**
   * Constructs an available commands event.
   *
   * @param player the targeted player
   * @param rootNode the Brigadier root node
   */
  public PlayerAvailableCommandsEvent(Player player,
      RootCommandNode<?> rootNode) {
    this.player = checkNotNull(player, "player");
    this.rootNode = checkNotNull(rootNode, "rootNode");
  }

  public Player getPlayer() {
    return player;
  }

  public RootCommandNode<?> getRootNode() {
    return rootNode;
  }
}
