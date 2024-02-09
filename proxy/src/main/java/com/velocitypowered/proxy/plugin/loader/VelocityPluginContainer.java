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

package com.velocitypowered.proxy.plugin.loader;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements {@link PluginContainer}.
 */
public class VelocityPluginContainer implements PluginContainer {

  private final PluginDescription description;
  private Object instance;
  private volatile ExecutorService service;

  public VelocityPluginContainer(PluginDescription description) {
    this.description = description;
  }

  @Override
  public PluginDescription getDescription() {
    return this.description;
  }

  @Override
  public Optional<?> getInstance() {
    return Optional.ofNullable(instance);
  }

  public void setInstance(Object instance) {
    this.instance = instance;
  }

  @Override
  public ExecutorService getExecutorService() {
    if (this.service == null) {
      synchronized (this) {
        if (this.service == null) {
          String name = this.description.getName().orElse(this.description.getId());
          this.service = Executors.unconfigurableExecutorService(
              Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true)
                    .setNameFormat(name + " - Task Executor #%d")
                    .setDaemon(true)
                    .build()
              )
          );
        }
      }
    }

    return this.service;
  }

  public boolean hasExecutorService() {
    return this.service != null;
  }
}
