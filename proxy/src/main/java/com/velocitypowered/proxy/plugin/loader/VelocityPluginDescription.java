package com.velocitypowered.proxy.plugin.loader;

import com.google.common.collect.Maps;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class VelocityPluginDescription implements PluginDescription {
    private final String id;
    private final String version;
    private final String author;
    private final Map<String, PluginDependency> dependencies;
    private final Path source;

    public VelocityPluginDescription(String id, String version, String author, Collection<PluginDependency> dependencies, Path source) {
        this.id = checkNotNull(id, "id");
        this.version = checkNotNull(version, "version");
        this.author = checkNotNull(author, "author");
        this.dependencies = Maps.uniqueIndex(dependencies, PluginDependency::getId);
        this.source = source;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getAuthor() {
        return author;
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
        return "VelocityPluginDescription{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", dependencies=" + dependencies +
                ", source=" + source +
                '}';
    }
}
