package com.velocitypowered.api.command;

import net.kyori.text.Component;

import javax.annotation.Nonnull;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource {
    /**
     * Sends the specified {@code component} to the invoker.
     * @param component the text component to send
     */
    void sendMessage(@Nonnull Component component);

    /**
     * Determines whether or not the invoker has a particular permission.
     * @param permission the permission to check for
     * @return whether or not the invoker has permission to run this command
     */
    boolean hasPermission(@Nonnull String permission);
}
