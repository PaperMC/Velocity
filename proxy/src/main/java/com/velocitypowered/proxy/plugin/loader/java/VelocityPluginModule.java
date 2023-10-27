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

package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VelocityPluginModule implements Module {

  private final ProxyServer server;
  private final JavaVelocityPluginDescription description;
  private final PluginContainer pluginContainer;
  private final Path basePluginPath;

  VelocityPluginModule(ProxyServer server, JavaVelocityPluginDescription description,
      PluginContainer pluginContainer, Path basePluginPath) {
    this.server = server;
    this.description = description;
    this.pluginContainer = pluginContainer;
    this.basePluginPath = basePluginPath;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(description.getMainClass()).in(Scopes.SINGLETON);

    binder.bind(Logger.class).toInstance(LoggerFactory.getLogger(description.getId()));
    binder.bind(ComponentLogger.class).toInstance(ComponentLogger.logger(description.getId()));
    binder.bind(Path.class).annotatedWith(DataDirectory.class)
        .toInstance(basePluginPath.resolve(description.getId()));
    binder.bind(PluginDescription.class).toInstance(description);
    binder.bind(PluginContainer.class).toInstance(pluginContainer);

    binder.bind(ExecutorService.class).toProvider(pluginContainer::getExecutorService);
  }
}
