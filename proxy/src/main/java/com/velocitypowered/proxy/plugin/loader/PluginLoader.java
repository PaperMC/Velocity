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

import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import java.nio.file.Path;

/**
 * This interface is used for loading plugins.
 */
public interface PluginLoader {

  /**
   * Loads a candidate description from the given {@code source}.
   *
   * @param source the source to load the candidate from
   * @return a plugin candidate description
   * @throws Exception if anything goes wrong
   */
  PluginDescription loadCandidate(Path source) throws Exception;

  /**
   * Materializes a "real" plugin description from the given {@code candidate}.
   *
   * @param candidate the candidate to materialize
   * @return a plugin description
   * @throws Exception if anything goes wrong
   */
  PluginDescription createPluginFromCandidate(PluginDescription candidate) throws Exception;

  /**
   * Creates a {@link Module} for the provided {@link PluginContainer} and verifies that the
   * container's {@link PluginDescription} is correct.
   *
   * <p>Does not create an instance of the plugin.</p>
   *
   * @param container the plugin container
   * @return the module containing bindings specific to this plugin
   * @throws IllegalArgumentException If the {@link PluginDescription} is missing the path
   */
  Module createModule(PluginContainer container) throws Exception;

  /**
   * Creates an instance of the plugin as specified by the plugin's main class found in the
   * {@link PluginDescription}.
   *
   * <p>The provided {@link Module modules} are used to create an
   * injector which is then used to create the plugin instance.</p>
   *
   * <p>The plugin instance is set in the provided {@link PluginContainer}.</p>
   *
   * @param container the plugin container
   * @param modules   the modules to be used when creating this plugin's injector
   * @throws IllegalStateException If the plugin instance could not be created from the provided
   *                               modules
   */
  void createPlugin(PluginContainer container, Module... modules) throws Exception;
}
