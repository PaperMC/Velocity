package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;

import java.util.Optional;

public class VelocityPluginContainer implements PluginContainer {
    private final PluginDescription description;
    private final Object instance;

    public VelocityPluginContainer(PluginDescription description, Object instance) {
        this.description = description;
        this.instance = instance;
    }

    @Override
    public PluginDescription getDescription() {
        return this.description;
    }

    @Override
    public Optional<?> getInstance() {
        return Optional.ofNullable(instance);
    }
}
