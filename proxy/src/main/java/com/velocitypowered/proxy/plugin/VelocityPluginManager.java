package com.velocitypowered.proxy.plugin;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.loader.JavaPluginLoader;
import com.velocitypowered.proxy.plugin.util.DirectedAcyclicGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final Map<Object, PluginContainer> pluginInstances = new HashMap<>();
    private final VelocityServer server;

    public VelocityPluginManager(VelocityServer server) {
        this.server = checkNotNull(server, "server");
    }

    private void registerPlugin(PluginContainer plugin) {
        plugins.put(plugin.getId(), plugin);
        plugin.getInstance().ifPresent(instance -> pluginInstances.put(instance, plugin));
    }

    public void loadPlugins(Path directory) throws IOException {
        checkNotNull(directory, "directory");
        checkArgument(Files.isDirectory(directory), "provided path isn't a directory");

        List<PluginDescription> found = new ArrayList<>();
        JavaPluginLoader loader = new JavaPluginLoader(server);

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

        List<PluginDescription> sortedPlugins = sortDescriptions(found);

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

    List<PluginDescription> sortDescriptions(List<PluginDescription> descriptions) {
        // Create our graph, we're going to be using this for Kahn's algorithm.
        DirectedAcyclicGraph<PluginDescription> graph = new DirectedAcyclicGraph<>();

        // Add edges
        for (PluginDescription description : descriptions) {
            graph.add(description);

            for (PluginDependency dependency : description.getDependencies()) {
                Optional<PluginDescription> in = descriptions.stream().filter(d -> d.getId().equals(dependency.getId())).findFirst();

                if (in.isPresent()) {
                    graph.addEdges(description, in.get());
                }
            }
        }

        // Find nodes that have no edges
        Queue<DirectedAcyclicGraph.Node<PluginDescription>> noEdges = graph.getNodesWithNoEdges();

        // Actually run Kahn's algorithm
        List<PluginDescription> sorted = new ArrayList<>();
        while (!noEdges.isEmpty()) {
            DirectedAcyclicGraph.Node<PluginDescription> descriptionNode = noEdges.poll();
            PluginDescription description = descriptionNode.getData();
            sorted.add(description);

            for (DirectedAcyclicGraph.Node<PluginDescription> node : graph.withEdge(description)) {
                node.removeEdge(descriptionNode);

                if (node.getAdjacent().isEmpty()) {
                    if (!noEdges.contains(node)) {
                        noEdges.add(node);
                    }
                }
            }
        }

        if (graph.hasEdges()) {
            throw new IllegalStateException("Plugin circular dependency found: " + graph.toString());
        }

        return sorted;
    }

    @Override
    public Optional<PluginContainer> fromInstance(Object instance) {
        checkNotNull(instance, "instance");

        if (instance instanceof PluginContainer) {
            return Optional.of((PluginContainer) instance);
        }

        return Optional.ofNullable(pluginInstances.get(instance));
    }

    @Override
    public Optional<PluginContainer> getPlugin(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    @Override
    public Collection<PluginContainer> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    @Override
    public boolean isLoaded(String id) {
        return plugins.containsKey(id);
    }
}
