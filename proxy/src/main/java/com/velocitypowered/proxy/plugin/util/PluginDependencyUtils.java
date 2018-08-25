package com.velocitypowered.proxy.plugin.util;

import com.google.common.collect.Maps;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.util.*;

public class PluginDependencyUtils {
    public static List<PluginDescription> sortCandidates(List<PluginDescription> candidates) {
        // Create our graph, we're going to be using this for Kahn's algorithm.
        MutableGraph<PluginDescription> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
        Map<String, PluginDescription> candidateMap = Maps.uniqueIndex(candidates, PluginDescription::getId);

        // Add edges
        for (PluginDescription description : candidates) {
            graph.addNode(description);

            for (PluginDependency dependency : description.getDependencies()) {
                PluginDescription in = candidateMap.get(dependency.getId());

                if (in != null) {
                    graph.putEdge(description, in);
                }
            }
        }

        // Find nodes that have no edges
        Queue<PluginDescription> noEdges = getNoDependencyCandidates(graph);

        // Actually run Kahn's algorithm
        List<PluginDescription> sorted = new ArrayList<>();
        while (!noEdges.isEmpty()) {
            PluginDescription candidate = noEdges.poll();
            sorted.add(candidate);

            for (PluginDescription node : graph.successors(candidate)) {
                graph.removeEdge(node, candidate);

                if (graph.adjacentNodes(node).isEmpty()) {
                    if (!noEdges.contains(node)) {
                        noEdges.add(node);
                    }
                }
            }
        }

        if (!graph.edges().isEmpty()) {
            throw new IllegalStateException("Plugin circular dependency found: " + graph.toString());
        }

        return sorted;
    }

    public static Queue<PluginDescription> getNoDependencyCandidates(Graph<PluginDescription> graph) {
        Queue<PluginDescription> found = new ArrayDeque<>();

        for (PluginDescription node : graph.nodes()) {
            if (graph.outDegree(node) == 0) {
                found.add(node);
            }
        }

        return found;
    }
}
