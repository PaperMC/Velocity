package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.meta.PluginDependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

public class VelocityPluginContainer extends VelocityPluginDescription implements PluginContainer {
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
        // TODO Figure out how to use Log4j logger
        return Logger.getLogger(getId());
    }
}
