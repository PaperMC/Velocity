package com.velocitypowered.proxy.plugin.util;

import com.google.common.collect.Maps;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginDependencyUtils {

  private PluginDependencyUtils() {
    throw new AssertionError();
  }

  /**
   * Attempts to topographically sort all plugins for the proxy to load by dependencies using
   * a depth-first search.
   *
   * @param candidates the plugins to sort
   * @return the sorted list of plugins
   * @throws IllegalStateException if there is a circular loop in the dependency graph
   */
  public static List<PluginDescription> sortCandidates(List<PluginDescription> candidates) {
    // Create a graph and populate it with plugin dependencies. Specifically, each graph has plugin
    // nodes, and edges that represent the dependencies that plugin relies on. Non-existent plugins
    // are ignored.
    MutableGraph<PluginDescription> graph = GraphBuilder.directed()
        .allowsSelfLoops(false)
        .expectedNodeCount(candidates.size())
        .build();
    Map<String, PluginDescription> candidateMap = Maps
        .uniqueIndex(candidates, d -> d == null ? null : d.getId());

    for (PluginDescription description : candidates) {
      graph.addNode(description);

      for (PluginDependency dependency : description.getDependencies()) {
        PluginDescription in = candidateMap.get(dependency.getId());

        if (in != null) {
          graph.putEdge(description, in);
        }
      }
    }

    // Now we do the depth-first search.
    List<PluginDescription> sorted = new ArrayList<>();
    Map<PluginDescription, Mark> marks = new HashMap<>();

    for (PluginDescription node : graph.nodes()) {
      visitNode(graph, node, marks, sorted, new ArrayDeque<>());
    }

    return sorted;
  }

  private static void visitNode(Graph<PluginDescription> dependencyGraph, PluginDescription node,
      Map<PluginDescription, Mark> marks, List<PluginDescription> sorted,
      Deque<PluginDescription> currentIteration) {
    Mark mark = marks.getOrDefault(node, Mark.NOT_VISITED);
    if (mark == Mark.PERMANENT) {
      return;
    } else if (mark == Mark.TEMPORARY) {
      // A circular dependency has been detected.
      currentIteration.addLast(node);
      StringBuilder loopGraph = new StringBuilder();
      for (PluginDescription description : currentIteration) {
        loopGraph.append(description.getId());
        loopGraph.append(" -> ");
      }
      loopGraph.setLength(loopGraph.length() - 4);
      throw new IllegalStateException("Circular dependency detected: " + loopGraph.toString());
    }

    currentIteration.addLast(node);
    marks.put(node, Mark.TEMPORARY);
    for (PluginDescription edge : dependencyGraph.successors(node)) {
      visitNode(dependencyGraph, edge, marks, sorted, currentIteration);
    }

    marks.put(node, Mark.PERMANENT);
    currentIteration.removeLast();
    sorted.add(node);
  }

  private enum Mark {
    NOT_VISITED,
    TEMPORARY,
    PERMANENT
  }
}
