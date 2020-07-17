package com.velocitypowered.api.command;

/**
 * A specialized sub-interface of {@code Command} which indicates that the proxy should pass
 * a raw command to the command. This is useful for bolting on external command frameworks to
 * Velocity.
 */
public interface RawCommand extends Command<RawCommandInvocation> {

}
