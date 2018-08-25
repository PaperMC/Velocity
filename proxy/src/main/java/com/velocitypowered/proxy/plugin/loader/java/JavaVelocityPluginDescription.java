package com.velocitypowered.proxy.plugin.loader.java;

import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaVelocityPluginDescription extends VelocityPluginDescription {
    private final Class<?> mainClass;

    public JavaVelocityPluginDescription(String id, @Nullable String name, @Nullable String version, @Nullable String description, @Nullable String url,
            @Nullable List<String> authors, Collection<PluginDependency> dependencies, Path source, Class<?> mainClass) {
        super(id, name, version, description, url, authors, dependencies, source);
        this.mainClass = checkNotNull(mainClass);
    }

    public Class<?> getMainClass() {
        return mainClass;
    }
}
