/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.player.java.JavaClientSettings;

public interface PlayerClientSettingsChangedEvent extends Event {

  Player player();

  JavaClientSettings settings();
}
