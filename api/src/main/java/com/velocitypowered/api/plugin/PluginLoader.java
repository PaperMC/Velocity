package com.velocitypowered.api.plugin;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * This interface is used for loading plugins.
 */
public interface PluginLoader {
    @Nonnull
    PluginCandidate loadPlugin(Path source) throws Exception;

    @Nonnull
    PluginContainer createPlugin(PluginCandidate plugin) throws Exception;
}
