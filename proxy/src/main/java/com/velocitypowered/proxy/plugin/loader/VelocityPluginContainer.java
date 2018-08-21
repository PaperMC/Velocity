package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class VelocityPluginContainer extends VelocityPluginDescription implements PluginContainer {
    private final Object instance;

    public VelocityPluginContainer(String id, String version, List<String> authors, Collection<PluginDependency> dependencies, Path source, Object instance) {
        super(id, version, authors, dependencies, source);
        this.instance = instance;
    }

    @Override
    public PluginDescription getDescription() {
        return this;
    }

    @Override
    public Optional<?> getInstance() {
        return Optional.ofNullable(instance);
    }
}
