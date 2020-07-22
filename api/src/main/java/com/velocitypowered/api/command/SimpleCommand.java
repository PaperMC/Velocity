package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A simple command, modelled after the convention popularized by
 * Bukkit and BungeeCord.
 *
 * <p>Prefer using {@link BrigadierCommand}, which is also
 * backwards-compatible with older clients.
 */
public interface SimpleCommand extends InvocableCommand<SimpleCommand.Invocation> {

  /**
   * Contains the invocation data for a simple command.
   */
  interface Invocation extends CommandInvocation<String @NonNull []> {

  }
}
