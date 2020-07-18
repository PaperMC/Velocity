package com.velocitypowered.api.command;

/**
 * A legacy command, modelled after the convention popularized by
 * Bukkit and BungeeCord.
 *
 * <p>Prefer using {@link BrigadierCommand} if possible, which is also
 * backwards-compatible with older clients.
 */
public interface LegacyCommand extends Command<LegacyCommandInvocation> {

}
