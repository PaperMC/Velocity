package com.velocitypowered.proxy.plugin;

import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.loader.JavaPluginLoader;
import com.velocitypowered.proxy.plugin.util.PluginDependencyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class VelocityPluginManager implements PluginManager {
    private static final Logger logger = LogManager.getLogger(VelocityPluginManager.class);

    private final Map<String, PluginContainer> plugins = new HashMap<>();
    private final Map<Object, PluginContainer> pluginInstances = new IdentityHashMap<>();
    private final VelocityServer server;

    public VelocityPluginManager(VelocityServer server) {
        this.server = checkNotNull(server, "server");
    }

    private void registerPlugin(@NonNull PluginContainer plugin) {
        plugins.put(plugin.getDescription().getId(), plugin);
        plugin.getInstance().ifPresent(instance -> pluginInstances.put(instance, plugin));
    }

    public void loadPlugins(@NonNull Path directory) throws IOException {
        checkNotNull(directory, "directory");
        checkArgument(Files.isDirectory(directory), "provided path isn't a directory");

        List<PluginDescription> found = new ArrayList<>();
        JavaPluginLoader loader = new JavaPluginLoader(server, directory);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, p -> Files.isRegularFile(p) && p.toString().endsWith(".jar"))) {
            for (Path path : stream) {
                try {
                    found.add(loader.loadPlugin(path));
                } catch (Exception e) {
                    logger.error("Unable to load plugin {}", path, e);
                }
            }
        }

        if (found.isEmpty()) {
            // No plugins found
            return;
        }

        List<PluginDescription> sortedPlugins = PluginDependencyUtils.sortCandidates(found);

        // Now load the plugins
        pluginLoad:
        for (PluginDescription plugin : sortedPlugins) {
            // Verify dependencies
            for (PluginDependency dependency : plugin.getDependencies()) {
                if (!dependency.isOptional() && !isLoaded(dependency.getId())) {
                    logger.error("Can't load plugin {} due to missing dependency {}", plugin.getId(), dependency.getId());
                    continue pluginLoad;
                }
            }

            // Actually create the plugin
            PluginContainer pluginObject;

            try {
                pluginObject = loader.createPlugin(plugin);
            } catch (Exception e) {
                logger.error("Can't create plugin {}", plugin.getId(), e);
                continue;
            }

            registerPlugin(pluginObject);
        }
    }

    @Override
    public @NonNull Optional<PluginContainer> fromInstance(@NonNull Object instance) {
        checkNotNull(instance, "instance");

        if (instance instanceof PluginContainer) {
            return Optional.of((PluginContainer) instance);
        }

        return Optional.ofNullable(pluginInstances.get(instance));
    }

    @Override
    public @NonNull Optional<PluginContainer> getPlugin(@NonNull String id) {
        checkNotNull(id, "id");
        return Optional.ofNullable(plugins.get(id));
    }

    @Override
    public @NonNull Collection<PluginContainer> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    @Override
    public boolean isLoaded(@NonNull String id) {
        return plugins.containsKey(id);
    }

    @Override
    public void addToClasspath(@NonNull Object plugin, @NonNull Path path) {
        checkNotNull(plugin, "instance");
        checkNotNull(path, "path");
        checkArgument(pluginInstances.containsKey(plugin), "plugin is not loaded");

        ClassLoader pluginClassloader = plugin.getClass().getClassLoader();
        if (pluginClassloader instanceof PluginClassLoader) {
            ((PluginClassLoader) pluginClassloader).addPath(path);
        } else {
            throw new UnsupportedOperationException("Operation is not supported on non-Java Velocity plugins.");
        }
    }
}
