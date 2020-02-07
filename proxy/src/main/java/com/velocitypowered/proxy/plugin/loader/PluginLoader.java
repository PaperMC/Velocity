package com.velocitypowered.proxy.plugin.loader;

import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;

import java.nio.file.Path;

/**
 * This interface is used for loading plugins.
 */
public interface PluginLoader {

  PluginDescription loadPlugin(Path source) throws Exception;

  /**
   * Creates a {@link Module} for the provided {@link PluginContainer}
   * and verifies that the container's {@link PluginDescription} is correct.
   *
   * <p>Does not create an instance of the plugin.</p>
   *
   * @param container the plugin container
   * @return the module containing bindings specific to this plugin
   * @throws IllegalArgumentException If the {@link PluginDescription}
   *                                  is missing the path
   */
  Module createModule(PluginContainer container) throws Exception;

  /**
   * Creates an instance of the plugin as specified by the
   * plugin's main class found in the {@link PluginDescription}.
   *
   * <p>The provided {@link Module modules} are used to create an
   * injector which is then used to create the plugin instance.</p>
   *
   * <p>The plugin instance is set in the provided {@link PluginContainer}.</p>
   *
   * @param container the plugin container
   * @param modules   the modules to be used when creating this plugin's injector
   * @throws IllegalStateException If the plugin instance could not be
   *                               created from the provided modules
   */
  void createPlugin(PluginContainer container, Module... modules) throws Exception;
}
