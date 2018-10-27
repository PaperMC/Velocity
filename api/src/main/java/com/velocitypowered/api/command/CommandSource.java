package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import net.kyori.text.Component;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends PermissionSubject {
    /**
     * Sends the specified {@code component} to the invoker.
     * @param component the text component to send
     */
    void sendMessage(Component component);
}
