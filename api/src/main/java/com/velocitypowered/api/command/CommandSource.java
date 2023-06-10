/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.permission.PermissionSubject;
import net.kyori.adventure.audience.Audience;

/**
 * Represents something that can be used to run a {@link Command}.
 */
public interface CommandSource extends Audience, PermissionSubject {

}
