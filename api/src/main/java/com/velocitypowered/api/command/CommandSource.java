package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import net.kyori.adventure.audience.Audience;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends Audience, PermissionSubject {
}
