package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class VelocityPluginContainer extends VelocityPluginCandidate implements PluginContainer {
    private final Optional<?> instance;

    public VelocityPluginContainer(String id, String version, String author, Collection<PluginDependency> dependencies, Path source, Object instance) {
        super(id, version, author, dependencies, source);
        this.instance = Optional.ofNullable(instance);
    }

    @Override
    public Optional<?> getInstance() {
        return instance;
    }

    @Override
    public Logger getLogger() {
        return LoggerFactory.getLogger(getId());
    }
}
