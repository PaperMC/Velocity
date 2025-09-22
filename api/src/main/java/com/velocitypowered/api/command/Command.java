/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.proxy.Player;

/**
 * Represents a command that can be executed by a {@link CommandSource}
 * such as a {@link Player} or the console.
 *
 * <p><strong>You must not subclass <code>Command</code></strong>. Use one of the following
 * <i>registrable</i> subinterfaces:</p>
 *
 * <ul>
 * <li>{@link BrigadierCommand}, which supports parameterized arguments and
 * specialized execution, tab complete suggestions and permission-checking logic.
 *
 * <li>{@link SimpleCommand}, modelled after the convention popularized by
 * Bukkit and BungeeCord. Older classes directly implementing {@link Command}
 * are suggested to migrate to this interface.
 *
 * <li>{@link RawCommand}, useful for bolting on external command frameworks
 * to Velocity.
 *
 * </ul>
 */
public sealed interface Command permits BrigadierCommand, InvocableCommand {
}
