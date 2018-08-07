package com.velocitypowered.proxy.plugin.util;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.velocitypowered.api.plugin.PluginCandidate;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.util.*;

public class PluginDependencyUtils {
    public static List<PluginCandidate> sortCandidates(List<PluginCandidate> candidates) {
        // Create our graph, we're going to be using this for Kahn's algorithm.
        MutableGraph<PluginCandidate> graph = GraphBuilder.directed().allowsSelfLoops(false).build();

        // Add edges
        for (PluginCandidate description : candidates) {
            graph.addNode(description);

            for (PluginDependency dependency : description.getDependencies()) {
                Optional<PluginCandidate> in = candidates.stream().filter(d -> d.getId().equals(dependency.getId())).findFirst();

                if (in.isPresent()) {
                    graph.putEdge(description, in.get());
                }
            }
        }

        // Find nodes that have no edges
        Queue<PluginCandidate> noEdges = getNoDependencyCandidates(graph);

        // Actually run Kahn's algorithm
        List<PluginCandidate> sorted = new ArrayList<>();
        while (!noEdges.isEmpty()) {
            PluginCandidate candidate = noEdges.poll();
            sorted.add(candidate);

            for (PluginCandidate node : graph.successors(candidate)) {
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

    public static Queue<PluginCandidate> getNoDependencyCandidates(Graph<PluginCandidate> graph) {
        Queue<PluginCandidate> found = new ArrayDeque<>();

        for (PluginCandidate node : graph.nodes()) {
            if (graph.outDegree(node) == 0) {
                found.add(node);
            }
        }

        return found;
    }
}
