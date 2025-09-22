/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;

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
  public BrigadierCommand(final @NotNull LiteralArgumentBuilder<CommandSource> builder) {
    this(Preconditions.checkNotNull(builder, "builder").build());
  }

  /**
   * Constructs a {@link BrigadierCommand} from the given command node.
   *
   * @param node the command node
   */
  public BrigadierCommand(final @NotNull LiteralCommandNode<CommandSource> node) {
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

  /**
   * Creates a new LiteralArgumentBuilder of the required name.
   *
   * @param name the literal name.
   * @return a new LiteralArgumentBuilder.
   */
  public static LiteralArgumentBuilder<CommandSource> literalArgumentBuilder(
          final @NotNull String name) {
    Preconditions.checkNotNull(name, "name");
    // Validation to avoid beginner's errors in case someone includes a space in the argument name
    Preconditions.checkArgument(name.indexOf(' ') == -1, "the argument name cannot contain spaces");
    return LiteralArgumentBuilder.literal(name);
  }

  /**
   * Creates a new RequiredArgumentBuilder of the required name and type.
   *
   * @param name the argument name
   * @param argumentType the argument type required
   * @param <T> the ArgumentType required type
   * @return a new RequiredArgumentBuilder
   */
  public static <T> RequiredArgumentBuilder<CommandSource, T> requiredArgumentBuilder(
          final @NotNull String name, @NotNull final ArgumentType<T> argumentType) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(argumentType, "argument type");

    return RequiredArgumentBuilder.argument(name, argumentType);
  }
}
