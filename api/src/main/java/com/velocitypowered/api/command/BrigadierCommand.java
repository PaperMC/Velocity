/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * A command that uses Brigadier for parsing the command and
 * providing suggestions to the client.
 */
public final class BrigadierCommand implements Command {

  /**
   * The return code used by a {@link com.mojang.brigadier.Command} to indicate
   * the command execution should be forwarded to the backend server.
   */
  public static final int FORWARD = 0xF6287429;

  private final LiteralCommandNode<CommandSource> node;

  /**
   * Constructs a {@link BrigadierCommand} from the node returned by
   * the given builder.
   *
   * @param builder the {@link LiteralCommandNode} builder
   */
  public BrigadierCommand(final LiteralArgumentBuilder<CommandSource> builder) {
    this(Preconditions.checkNotNull(builder, "builder").build());
  }

  /**
   * Constructs a {@link BrigadierCommand} from the given command node.
   *
   * @param node the command node
   */
  public BrigadierCommand(final LiteralCommandNode<CommandSource> node) {
    this.node = Preconditions.checkNotNull(node, "node");
  }

  /**
   * Returns the literal node for this command.
   *
   * @return the command node
   */
  public LiteralCommandNode<CommandSource> getNode() {
    return node;
  }
}
