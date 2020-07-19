package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A legacy command, modelled after the convention popularized by
 * Bukkit and BungeeCord.
 *
 * <p>Prefer using {@link BrigadierCommand} if possible, which is also
 * backwards-compatible with older clients.
 */
public interface LegacyCommand extends InvocableCommand<LegacyCommand.Invocation> {

  /**
   * Contains the invocation data for a legacy command.
   */
  interface Invocation extends CommandInvocation<String @NonNull []> {

  }
}
