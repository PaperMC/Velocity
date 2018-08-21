package com.velocitypowered.proxy.plugin.loader;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class VelocityPluginDescription implements PluginDescription {
    private final String id;
    private final @Nullable String name;
    private final @Nullable String version;
    private final @Nullable String url;
    private final List<String> authors;
    private final Map<String, PluginDependency> dependencies;
    private final Path source;

    public VelocityPluginDescription(String id, @Nullable String name, @Nullable String version, @Nullable String url,
            @Nullable List<String> authors, Collection<PluginDependency> dependencies, Path source) {
        this.id = checkNotNull(id, "id");
        this.name = Strings.emptyToNull(name);
        this.version = Strings.emptyToNull(version);
        this.url = Strings.emptyToNull(url);
        this.authors = authors == null ? ImmutableList.of() : ImmutableList.copyOf(authors);
        this.dependencies = Maps.uniqueIndex(dependencies, PluginDependency::getId);
        this.source = source;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Override
    public List<String> getAuthors() {
        return authors;
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
                ", authors='" + authors + '\'' +
                ", dependencies=" + dependencies +
                ", source=" + source +
                '}';
    }
}
