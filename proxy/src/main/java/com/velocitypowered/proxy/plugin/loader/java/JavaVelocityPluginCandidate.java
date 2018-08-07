package com.velocitypowered.proxy.plugin.loader.java;

import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginCandidate;

import java.nio.file.Path;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaVelocityPluginCandidate extends VelocityPluginCandidate {
    private final Class mainClass;

    public JavaVelocityPluginCandidate(String id, String version, String author, Collection<PluginDependency> dependencies, Path source, Class mainClass) {
        super(id, version, author, dependencies, source);
        this.mainClass = checkNotNull(mainClass);
    }

    public Class getMainClass() {
        return mainClass;
    }
}
