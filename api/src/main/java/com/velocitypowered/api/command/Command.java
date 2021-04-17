/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.proxy.connection.Player;

/**
 * Represents a command that can be executed by a {@link CommandSource}
 * such as a {@link Player} or the console.
 *
 * <p><strong>You should not subclass <code>Command</code></strong>. Use one of the following
 * subinterfaces:</p>
 *
 * <ul>
 * <li>{@link BrigadierCommand} wraps a Brigadier literal command node. It supports parameterized
 * arguments and specialized execution, tab complete suggestions and permission-checking logic.
 *
 * <li>{@link SimpleCommand} is modelled after the convention popularized by Bukkit and BungeeCord.
 *
 * <li>{@link RawCommand} is useful for bolting on external command frameworks onto Velocity.
 *
 * </ul>
 */
public interface Command {
}
