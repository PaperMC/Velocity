package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.velocitypowered.api.plugin.annotations.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class VelocityPluginModule implements Module {
    private final JavaVelocityPluginDescription description;
    private final Path basePluginPath;

    public VelocityPluginModule(JavaVelocityPluginDescription description, Path basePluginPath) {
        this.description = description;
        this.basePluginPath = basePluginPath;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Logger.class).toInstance(LoggerFactory.getLogger(description.getId()));
        binder.bind(ProxyServer.class).toInstance(VelocityServer.getServer());
        binder.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(basePluginPath.resolve(description.getId()));
    }
}
