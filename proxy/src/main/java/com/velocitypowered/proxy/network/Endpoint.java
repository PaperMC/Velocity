/*
 * Copyright (C) 2020-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.network;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ListenerType;
import io.netty.channel.Channel;

/**
 * Represents a listener endpoint.
 */
public final class Endpoint {

  private final Channel channel;
  private final ListenerType type;

  public Endpoint(Channel channel, ListenerType type) {
    this.channel = Preconditions.checkNotNull(channel, "channel");
    this.type = Preconditions.checkNotNull(type, "type");
  }

  public Channel getChannel() {
    return channel;
  }

  public ListenerType getType() {
    return type;
  }
}
