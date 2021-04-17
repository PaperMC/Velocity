/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.util.ModInfo;

/**
 * This event is fired when a modded client sends its mods to the proxy while connecting to a
 * server.
 */
public interface PlayerModInfoEvent {

  Player player();

  ModInfo modInfo();
}
