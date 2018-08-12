package com.velocitypowered.proxy.plugin.loader;

import com.google.common.collect.Maps;
import com.velocitypowered.api.plugin.PluginCandidate;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class VelocityPluginCandidate implements PluginCandidate {
    private final String id;
    private final String version;
    private final String author;
    private final Map<String, PluginDependency> dependencies;
    private final Path source;

    public VelocityPluginCandidate(String id, String version, String author, Collection<PluginDependency> dependencies, Path source) {
        this.id = id;
        this.version = version;
        this.author = author;
        this.dependencies = Maps.uniqueIndex(dependencies, PluginDependency::getId);
        this.source = source;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public Optional<String> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public Collection<PluginDependency> getDependencies() {
        return dependencies.values();
    }

    @Override
    public Optional<PluginDependency> getDependency(String id) {
        return Optional.ofNullable(dependencies.get(id));
    }

    @Override
    public Optional<Path> getSource() {
        return Optional.ofNullable(source);
    }

    @Override
    public String toString() {
        return "VelocityPluginCandidate{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", dependencies=" + dependencies +
                ", source=" + source +
                '}';
    }
}
