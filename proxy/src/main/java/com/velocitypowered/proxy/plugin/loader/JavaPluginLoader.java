package com.velocitypowered.proxy.plugin.loader;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.velocitypowered.api.plugin.*;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import com.velocitypowered.proxy.plugin.loader.java.JavaVelocityPluginDescription;
import com.velocitypowered.proxy.plugin.loader.java.SerializedPluginDescription;
import com.velocitypowered.proxy.plugin.loader.java.VelocityPluginModule;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

public class JavaPluginLoader implements PluginLoader {
    private final ProxyServer server;

    public JavaPluginLoader(ProxyServer server) {
        this.server = server;
    }

    @Nonnull
    @Override
    public PluginDescription loadPlugin(Path source) throws Exception {
        Optional<SerializedPluginDescription> serialized = getSerializedPluginInfo(source);

        if (!serialized.isPresent()) {
            throw new InvalidPluginException("Did not find a valid velocity-info.json.");
        }

        PluginClassLoader loader = new PluginClassLoader(
                new URL[] {source.toUri().toURL() }
        );

        Class mainClass = loader.loadClass(serialized.get().getMain());
        VelocityPluginDescription description = createDescription(serialized.get(), source, mainClass);

        String pluginId = description.getId();
        Pattern pattern = PluginDescription.ID_PATTERN;

        if (!pattern.matcher(pluginId).matches()) {
            throw new InvalidPluginException("Plugin ID '" + pluginId + "' must match pattern " + pattern.pattern());
        }

        return description;
    }

    @Nonnull
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

        Injector injector = Guice.createInjector(new VelocityPluginModule(javaDescription));
        Object instance = injector.getInstance(javaDescription.getMainClass());

        return new VelocityPluginContainer(
                description.getId(),
                description.getVersion(),
                description.getAuthor(),
                description.getDependencies(),
                source.get(),
                instance
        );
    }

    private Optional<SerializedPluginDescription> getSerializedPluginInfo(Path source) throws Exception {
        try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                if (entry.getName().equals("velocity-plugin.json")) {
                    try (Reader pluginInfoReader = new InputStreamReader(in)) {
                        return Optional.of(VelocityServer.GSON.fromJson(pluginInfoReader, SerializedPluginDescription.class));
                    }
                }
            }

            return Optional.empty();
        }
    }

    private VelocityPluginDescription createDescription(SerializedPluginDescription description, Path source, Class mainClass) {
        Set<PluginDependency> dependencies = new HashSet<>();

        for (SerializedPluginDescription.Dependency dependency : description.getDependencies()) {
            dependencies.add(toDependencyMeta(dependency));
        }

        return new JavaVelocityPluginDescription(
                description.getId(),
                description.getVersion(),
                description.getAuthor(),
                dependencies,
                source,
                mainClass
        );
    }

    private static PluginDependency toDependencyMeta(SerializedPluginDescription.Dependency dependency) {
        return new PluginDependency(
            dependency.getId(),
            null, // TODO Implement version matching in dependency annotation
            dependency.isOptional()
        );
    }
}
