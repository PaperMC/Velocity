package com.velocitypowered.proxy.plugin.loader.java;

import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;

import java.nio.file.Path;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Hugo Manrique
 * @since 07/08/2018
 */
public class JavaVelocityPluginDescription extends VelocityPluginDescription {
    private final Class mainClass;

    public JavaVelocityPluginDescription(String id, String version, String author, Collection<PluginDependency> dependencies, Path source, Class mainClass) {
        super(id, version, author, dependencies, source);
        this.mainClass = checkNotNull(mainClass);
    }

    public Class getMainClass() {
        return mainClass;
    }
}
