/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Backend channel initializer holder.
 */
public class BackendChannelInitializerHolder implements Supplier<ChannelInitializer<Channel>> {

  private static final Logger LOGGER = LogManager.getLogger(ConnectionManager.class);
  private ChannelInitializer<Channel> initializer;

  BackendChannelInitializerHolder(final ChannelInitializer<Channel> initializer) {
    this.initializer = initializer;
  }

  @Override
  public ChannelInitializer<Channel> get() {
    return this.initializer;
  }

  /**
   * Sets the channel initializer.
   *
   * @param initializer the new initializer to use
   * @deprecated Internal implementation detail
   */
  @Deprecated
  public void set(final ChannelInitializer<Channel> initializer) {
    LOGGER.warn("The backend channel initializer has been replaced by {}",
        Thread.currentThread().getStackTrace()[2]);
    this.initializer = initializer;
  }
}
