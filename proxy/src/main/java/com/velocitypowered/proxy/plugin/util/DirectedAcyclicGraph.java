package com.velocitypowered.proxy.plugin.util;

import com.google.common.collect.ImmutableList;

import java.util.*;

public class DirectedAcyclicGraph<T> {
    private final List<Node<T>> nodes = new ArrayList<>();

    public void addEdges(T one, T two) {
        Node<T> oneNode = add(one);
        Node<T> twoNode = add(two);

        oneNode.addEdge(twoNode);
    }

    public Node<T> add(T t) {
        Optional<Node<T>> willAdd = get(t);

        if (!willAdd.isPresent()) {
            Node<T> node = new Node<>(t);
            nodes.add(node);

            return node;
        } else {
            return willAdd.get();
        }
    }

    public Optional<Node<T>> get(T t) {
        for (Node<T> node : nodes) {
            if (node.data.equals(t)) {
                return Optional.of(node);
            }
        }

        return Optional.empty();
    }

    public List<Node<T>> withEdge(T t) {
        Optional<Node<T>> inOptional = get(t);

        if (!inOptional.isPresent()) {
            return ImmutableList.of();
        }

        Node<T> in = inOptional.get();
        List<Node<T>> list = new ArrayList<>();

        for (Node<T> node : nodes) {
            if (node.isAdjacent(in)) {
                list.add(node);
            }
        }

        return list;
    }

    public void remove(Node<T> node) {
        for (Node<T> tNode : nodes) {
            tNode.removeEdge(node);
        }

        nodes.remove(node);
    }

    public boolean hasEdges() {
        for (Node<T> node : nodes) {
            if (!node.adjacent.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public Queue<Node<T>> getNodesWithNoEdges() {
        Queue<Node<T>> found = new ArrayDeque<>();
        for (Node<T> node : nodes) {
            if (node.getAdjacent().isEmpty()) {
                found.add(node);
            }
        }
        return found;
    }

    @Override
    public String toString() {
        return "DirectedAcyclicGraph{" +
                "nodes=" + nodes +
                '}';
    }

    public static class Node<T> {
        private final T data;
        private final List<Node<T>> adjacent = new ArrayList<>(); // TODO Convert to Set?

        private Node(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public List<Node<T>> getAdjacent() {
            return adjacent;
        }

        public void addEdge(Node<T> edge) {
            if (!isAdjacent(edge)) {
                adjacent.add(edge);
            }
        }

        public void removeEdge(Node<T> edge) {
            adjacent.remove(edge);
        }

        public boolean isAdjacent(Node<T> edge) {
            return adjacent.contains(edge);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node<?> node = (Node<?>) o;
            return Objects.equals(data, node.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "data=" + data +
                    ", adjacent=" + adjacent +
                    '}';
        }
    }
}
