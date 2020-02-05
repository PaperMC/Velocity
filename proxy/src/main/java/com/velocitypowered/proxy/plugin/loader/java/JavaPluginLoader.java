package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.velocitypowered.api.plugin.InvalidPluginException;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.ap.SerializedPluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import com.velocitypowered.proxy.plugin.loader.PluginLoader;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import com.velocitypowered.proxy.plugin.loader.java.JavaVelocityPluginDescription;
import com.velocitypowered.proxy.plugin.loader.java.VelocityPluginModule;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JavaPluginLoader implements PluginLoader {

  private final ProxyServer server;
  private final Path baseDirectory;

  public JavaPluginLoader(ProxyServer server, Path baseDirectory) {
    this.server = server;
    this.baseDirectory = baseDirectory;
  }

  @Override
  public PluginDescription loadPlugin(Path source) throws Exception {
    Optional<SerializedPluginDescription> serialized = getSerializedPluginInfo(source);

    if (!serialized.isPresent()) {
      throw new InvalidPluginException("Did not find a valid velocity-plugin.json.");
    }

    SerializedPluginDescription pd = serialized.get();
    if (!SerializedPluginDescription.ID_PATTERN.matcher(pd.getId()).matches()) {
      throw new InvalidPluginException("Plugin ID '" + pd.getId() + "' is invalid.");
    }

    PluginClassLoader loader = new PluginClassLoader(
        new URL[]{source.toUri().toURL()}
    );
    loader.addToClassloaders();

    Class mainClass = loader.loadClass(pd.getMain());
    return createDescription(pd, source, mainClass);
  }

  @Override
  public PluginContainer createPlugin(PluginDescription description) throws Exception {
    if (!(description instanceof JavaVelocityPluginDescription)) {
      throw new IllegalArgumentException("Description provided isn't of the Java plugin loader");
    }

    JavaVelocityPluginDescription javaDescription = (JavaVelocityPluginDescription) description;
    Optional<Path> source = javaDescription.getSource();

    if (!source.isPresent()) {
      throw new IllegalArgumentException("No path in plugin description");
    }

    VelocityPluginContainer container = new VelocityPluginContainer(description);
    Injector injector = Guice
      .createInjector(new VelocityPluginModule(server, javaDescription, container, baseDirectory));
    Object instance = injector.getInstance(javaDescription.getMainClass());

    if (instance == null) {
      throw new IllegalStateException(
          "Got nothing from injector for plugin " + javaDescription.getId());
    }

    container.setInstance(instance);
    return container;
  }

  private Optional<SerializedPluginDescription> getSerializedPluginInfo(Path source)
      throws Exception {
    boolean foundBungeeBukkitPluginFile = false;
    try (JarInputStream in = new JarInputStream(
        new BufferedInputStream(Files.newInputStream(source)))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        if (entry.getName().equals("velocity-plugin.json")) {
          try (Reader pluginInfoReader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return Optional.of(VelocityServer.GSON
                .fromJson(pluginInfoReader, SerializedPluginDescription.class));
          }
        }

        if (entry.getName().equals("plugin.yml") || entry.getName().equals("bungee.yml")) {
          foundBungeeBukkitPluginFile = true;
        }
      }

      if (foundBungeeBukkitPluginFile) {
        throw new InvalidPluginException("The plugin file " + source.getFileName() + " appears to "
            + "be a Bukkit or BungeeCord plugin. Velocity does not support Bukkit or BungeeCord "
            + "plugins.");
      }

      return Optional.empty();
    }
  }

  private VelocityPluginDescription createDescription(SerializedPluginDescription description,
      Path source, Class mainClass) {
    Set<PluginDependency> dependencies = new HashSet<>();

    for (SerializedPluginDescription.Dependency dependency : description.getDependencies()) {
      dependencies.add(toDependencyMeta(dependency));
    }

    return new JavaVelocityPluginDescription(
        description.getId(),
        description.getName(),
        description.getVersion(),
        description.getDescription(),
        description.getUrl(),
        description.getAuthors(),
        dependencies,
        source,
        mainClass
    );
  }

  private static PluginDependency toDependencyMeta(
      SerializedPluginDescription.Dependency dependency) {
    return new PluginDependency(
        dependency.getId(),
        null, // TODO Implement version matching in dependency annotation
        dependency.isOptional()
    );
  }
}
