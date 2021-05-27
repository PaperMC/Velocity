/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.velocitypowered.annotationprocessor.SerializedPluginDescription;
import com.velocitypowered.api.plugin.InvalidPluginException;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import com.velocitypowered.proxy.plugin.loader.PluginLoader;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import io.leangen.geantyref.TypeToken;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JavaPluginLoader implements PluginLoader {

  private final Path baseDirectory;
  private final Map<URI, PluginClassLoader> classLoaders = new HashMap<>();

  public JavaPluginLoader(ProxyServer server, Path baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  @Override
  public Collection<PluginDescription> loadPluginCandidates(Path source) throws Exception {
    List<SerializedPluginDescription> serialized = getSerializedPluginInfo(source);
    if (serialized.isEmpty()) {
      throw new InvalidPluginException("Did not find a valid velocity-plugin-info.json.");
    }

    List<PluginDescription> candidates = new ArrayList<>();
    for (SerializedPluginDescription description : serialized) {
      candidates.add(createCandidateDescription(description, source));
    }
    return candidates;
  }

  @Override
  public PluginDescription materializePlugin(PluginDescription source) throws Exception {
    if (!(source instanceof JavaVelocityPluginDescriptionCandidate)) {
      throw new IllegalArgumentException("Description provided isn't of the Java plugin loader");
    }

    Path jarFilePath = source.file();
    if (jarFilePath == null) {
      throw new IllegalStateException("JAR path not provided.");
    }

    URI pluginJarUri = jarFilePath.toUri();
    URL pluginJarUrl = pluginJarUri.toURL();
    PluginClassLoader loader = this.classLoaders.computeIfAbsent(pluginJarUri, (uri) -> {
      PluginClassLoader classLoader = AccessController.doPrivileged(
          (PrivilegedAction<PluginClassLoader>) () -> new PluginClassLoader(new URL[]{pluginJarUrl},
              JavaPluginLoader.class.getClassLoader(), source));
      classLoader.addToClassloaders();
      return classLoader;
    });

    JavaVelocityPluginDescriptionCandidate candidate =
        (JavaVelocityPluginDescriptionCandidate) source;
    Class mainClass = loader.loadClass(candidate.getMainClass());
    return createDescription(candidate, mainClass);
  }

  @Override
  public Module createModule(PluginContainer container) throws Exception {
    PluginDescription description = container.description();
    if (!(description instanceof JavaVelocityPluginDescription)) {
      throw new IllegalArgumentException("Description provided isn't of the Java plugin loader");
    }

    JavaVelocityPluginDescription javaDescription = (JavaVelocityPluginDescription) description;
    Path source = javaDescription.file();

    if (source == null) {
      throw new IllegalArgumentException("No path in plugin description");
    }

    return new VelocityPluginModule(javaDescription, container, baseDirectory);
  }

  @Override
  public void createPlugin(PluginContainer container, Module... modules) {
    if (!(container instanceof VelocityPluginContainer)) {
      throw new IllegalArgumentException("Container provided isn't of the Java plugin loader");
    }
    PluginDescription description = container.description();
    if (!(description instanceof JavaVelocityPluginDescription)) {
      throw new IllegalArgumentException("Description provided isn't of the Java plugin loader");
    }

    Injector injector = Guice.createInjector(modules);
    Object instance = injector
        .getInstance(((JavaVelocityPluginDescription) description).getMainClass());

    if (instance == null) {
      throw new IllegalStateException(
        "Got nothing from injector for plugin " + description.id());
    }

    ((VelocityPluginContainer) container).setInstance(instance);
  }

  private List<SerializedPluginDescription> getSerializedPluginInfo(Path source)
      throws Exception {
    boolean foundOldVelocityPlugin = false;
    boolean foundBungeeBukkitPluginFile = false;
    try (JarInputStream in = new JarInputStream(
        new BufferedInputStream(Files.newInputStream(source)))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        if (entry.getName().equals("velocity-plugin-info.json")) {
          try (Reader pluginInfoReader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return VelocityServer.GENERAL_GSON.fromJson(pluginInfoReader,
                new TypeToken<List<SerializedPluginDescription>>() {}.getType());
          }
        }

        if (entry.getName().equals("velocity-plugin.json")) {
          foundOldVelocityPlugin = true;
        }

        if (entry.getName().equals("plugin.yml") || entry.getName().equals("bungee.yml")) {
          foundBungeeBukkitPluginFile = true;
        }
      }

      if (foundOldVelocityPlugin) {
        throw new InvalidPluginException("The plugin file " + source.getFileName() + " appears to "
            + "be developed for an older version of Velocity. Please obtain a newer version of the "
            + "plugin.");
      }

      if (foundBungeeBukkitPluginFile) {
        throw new InvalidPluginException("The plugin file " + source.getFileName() + " appears to "
            + "be a Bukkit or BungeeCord plugin. Velocity does not support Bukkit or BungeeCord "
            + "plugins.");
      }

      return List.of();
    }
  }

  private VelocityPluginDescription createCandidateDescription(
      SerializedPluginDescription description,
      Path source) {
    Set<PluginDependency> dependencies = new HashSet<>();

    for (SerializedPluginDescription.Dependency dependency : description.getDependencies()) {
      dependencies.add(toDependencyMeta(dependency));
    }

    return new JavaVelocityPluginDescriptionCandidate(
        description.getId(),
        description.getName(),
        description.getVersion(),
        description.getDescription(),
        description.getUrl(),
        description.getAuthors(),
        dependencies,
        source,
        description.getMain()
    );
  }

  private VelocityPluginDescription createDescription(
      JavaVelocityPluginDescriptionCandidate description,
      Class mainClass) {
    return new JavaVelocityPluginDescription(
        description.id(),
        description.name(),
        description.version(),
        description.description(),
        description.url(),
        description.authors(),
        description.dependencies(),
        description.file(),
        mainClass
    );
  }

  private static PluginDependency toDependencyMeta(
      SerializedPluginDescription.Dependency dependency) {
    return new PluginDependency(
        dependency.getId(),
        dependency.getVersion(),
        dependency.isOptional()
    );
  }
}
