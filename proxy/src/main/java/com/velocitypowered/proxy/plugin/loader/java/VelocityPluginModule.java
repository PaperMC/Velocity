package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class VelocityPluginModule implements Module {
    private final ProxyServer server;
    private final JavaVelocityPluginDescription description;
    private final Path basePluginPath;

    public VelocityPluginModule(ProxyServer server, JavaVelocityPluginDescription description, Path basePluginPath) {
        this.server = server;
        this.description = description;
        this.basePluginPath = basePluginPath;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Logger.class).toInstance(LoggerFactory.getLogger(description.getId()));
        binder.bind(ProxyServer.class).toInstance(server);
        binder.bind(Path.class).annotatedWith(DataDirectory.class).toInstance(basePluginPath.resolve(description.getId()));
        binder.bind(PluginDescription.class).toInstance(description);
        binder.bind(PluginManager.class).toInstance(server.getPluginManager());
        binder.bind(EventManager.class).toInstance(server.getEventManager());
        binder.bind(CommandManager.class).toInstance(server.getCommandManager());
    }
}
