package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;

import java.nio.file.Path;

/**
 * This interface is used for loading plugins.
 */
public interface PluginLoader {
    PluginDescription loadPlugin(Path source) throws Exception;

    PluginContainer createPlugin(PluginDescription plugin) throws Exception;
}
