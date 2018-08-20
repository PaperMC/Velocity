package com.velocitypowered.proxy.plugin.loader.java;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SerializedPluginDescription {
    private final String id;
    private final String author;
    private final String main;
    private final String version;
    private final List<Dependency> dependencies;

    public SerializedPluginDescription(String id, String author, String main, String version) {
        this(id, author, main, version, ImmutableList.of());
    }

    public SerializedPluginDescription(String id, String author, String main, String version, List<Dependency> dependencies) {
        this.id = Preconditions.checkNotNull(id, "id");
        this.author = Preconditions.checkNotNull(author, "author");
        this.main = Preconditions.checkNotNull(main, "main");
        this.version = Preconditions.checkNotNull(version, "version");
        this.dependencies = ImmutableList.copyOf(dependencies);
    }

    public static SerializedPluginDescription from(Plugin plugin, String qualifiedName) {
        List<Dependency> dependencies = new ArrayList<>();
        for (com.velocitypowered.api.plugin.Dependency dependency : plugin.dependencies()) {
            dependencies.add(new Dependency(dependency.id(), dependency.optional()));
        }
        return new SerializedPluginDescription(plugin.id(), plugin.author(), qualifiedName, plugin.version(), dependencies);
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getMain() {
        return main;
    }

    public String getVersion() {
        return version;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializedPluginDescription that = (SerializedPluginDescription) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(author, that.author) &&
                Objects.equals(main, that.main) &&
                Objects.equals(version, that.version) &&
                Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, main, version, dependencies);
    }

    @Override
    public String toString() {
        return "SerializedPluginDescription{" +
                "id='" + id + '\'' +
                ", author='" + author + '\'' +
                ", main='" + main + '\'' +
                ", version='" + version + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }

    public static class Dependency {
        private final String id;
        private final boolean optional;

        public Dependency(String id, boolean optional) {
            this.id = id;
            this.optional = optional;
        }

        public String getId() {
            return id;
        }

        public boolean isOptional() {
            return optional;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return optional == that.optional &&
                    Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, optional);
        }

        @Override
        public String toString() {
            return "Dependency{" +
                    "id='" + id + '\'' +
                    ", optional=" + optional +
                    '}';
        }
    }
}
