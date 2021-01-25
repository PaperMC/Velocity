package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VelocityPluginModule implements Module {

  private final ProxyServer server;
  private final JavaVelocityPluginDescription description;
  private final PluginContainer pluginContainer;
  private final Path basePluginPath;

  VelocityPluginModule(ProxyServer server, JavaVelocityPluginDescription description,
                       PluginContainer pluginContainer, Path basePluginPath) {
    this.server = server;
    this.description = description;
    this.pluginContainer = pluginContainer;
    this.basePluginPath = basePluginPath;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(description.getMainClass()).in(Scopes.SINGLETON);

    binder.bind(Logger.class).toInstance(LoggerFactory.getLogger(description.getId()));
    binder.bind(Path.class).annotatedWith(DataDirectory.class)
        .toInstance(basePluginPath.resolve(description.getId()));
    binder.bind(PluginDescription.class).toInstance(description);
    binder.bind(PluginContainer.class).toInstance(pluginContainer);
  }
}
