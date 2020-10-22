/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

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
