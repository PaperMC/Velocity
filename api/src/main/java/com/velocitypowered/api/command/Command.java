package com.velocitypowered.api.command;

import com.velocitypowered.api.proxy.connection.Player;

/**
 * Represents a command that can be executed by a {@link CommandSource}
 * such as a {@link Player} or the console.
 *
 * <p>Velocity 1.1.0 introduces specialized command subinterfaces to separate
 * command parsing concerns. These include, in order of preference:
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
public interface Command {
}
