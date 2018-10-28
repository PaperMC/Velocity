package com.velocitypowered.proxy.plugin.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class PluginDependencyUtils {

  private PluginDependencyUtils() {
    throw new AssertionError();
  }

  /**
   * Attempts to topographically sort all plugins for the proxy to load by dependencies using
   * Kahn's algorithm.
   *
   * @param candidates the plugins to sort
   * @return the sorted list of plugins
   * @throws IllegalStateException if there is a circular loop in the dependency graph
   */
  public static List<PluginDescription> sortCandidates(List<PluginDescription> candidates) {
    // Create our graph, we're going to be using this for Kahn's algorithm.
    MutableGraph<PluginDescription> graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    Map<String, PluginDescription> candidateMap = Maps
        .uniqueIndex(candidates, d -> d == null ? null : d.getId());

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
      PluginDescription candidate = noEdges.remove();
      sorted.add(candidate);

      for (PluginDescription node : ImmutableSet.copyOf(graph.adjacentNodes(candidate))) {
        graph.removeEdge(node, candidate);

        if (graph.adjacentNodes(node).isEmpty()) {
          noEdges.add(node);
        }
      }
    }

    if (!graph.edges().isEmpty()) {
      throw new IllegalStateException(
          "Plugin circular dependency found: " + createLoopInformation(graph));
    }

    return sorted;
  }

  private static Queue<PluginDescription> getNoDependencyCandidates(Graph<PluginDescription> graph) {
    Queue<PluginDescription> found = new ArrayDeque<>();

    for (PluginDescription node : graph.nodes()) {
      if (graph.outDegree(node) == 0) {
        found.add(node);
      }
    }

    return found;
  }

  private static String createLoopInformation(Graph<PluginDescription> graph) {
    StringBuilder repr = new StringBuilder("{");
    for (EndpointPair<PluginDescription> edge : graph.edges()) {
      repr.append(edge.target().getId()).append(": [");
      repr.append(dependencyLoopInfo(graph, edge.target(), new HashSet<>())).append("], ");
    }
    repr.setLength(repr.length() - 2);
    repr.append("}");
    return repr.toString();
  }

  private static String dependencyLoopInfo(Graph<PluginDescription> graph,
      PluginDescription dependency, Set<PluginDescription> seen) {
    StringBuilder repr = new StringBuilder();
    for (PluginDescription pd : graph.adjacentNodes(dependency)) {
      if (seen.add(pd)) {
        repr.append(pd.getId()).append(": [").append(dependencyLoopInfo(graph, dependency, seen))
            .append("], ");
      } else {
        repr.append(pd.getId()).append(", ");
      }
    }

    if (repr.length() != 0) {
      repr.setLength(repr.length() - 2);
      return repr.toString();
    } else {
      return "<no depends>";
    }
  }
}
