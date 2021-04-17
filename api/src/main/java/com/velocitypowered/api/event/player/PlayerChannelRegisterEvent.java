/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import java.util.List;

/**
 * This event is fired when a client ({@link Player}) sends a plugin message through the
 * register channel.
 */
public interface PlayerChannelRegisterEvent {

  Player player();

  List<PluginChannelId> channels();
}
