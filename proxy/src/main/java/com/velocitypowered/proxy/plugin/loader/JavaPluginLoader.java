package com.velocitypowered.proxy.plugin.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.velocitypowered.api.plugin.*;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import com.velocitypowered.proxy.plugin.loader.java.JavaVelocityPluginDescription;
import com.velocitypowered.proxy.plugin.loader.java.VelocityPluginModule;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
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
        String mainClassName = getMainClassName(source);

        if (mainClassName == null) {
            throw new AssertionError();
        }

        PluginClassLoader loader = new PluginClassLoader(
                new URL[] {source.toUri().toURL() }
        );

        Class mainClass = loader.loadClass(mainClassName);
        Annotation annotation = mainClass.getAnnotation(Plugin.class);

        if (!(annotation instanceof Plugin)) {
            throw new InvalidPluginException("Main class does not have @Plugin annotation");
        }

        VelocityPluginDescription description = createDescription((Plugin) annotation, source, mainClass);

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

    private String getMainClassName(Path source) throws Exception {
        try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                if (entry.getName().equals("velocity-plugin.json")) {
                    try (Reader pluginInfoReader = new InputStreamReader(in)) {
                        JsonObject pluginInfo = VelocityServer.GSON.fromJson(pluginInfoReader, JsonObject.class);
                        JsonElement mainClass = pluginInfo.get("main");
                        if (mainClass == null) {
                            throw new IllegalStateException("JAR's plugin info doesn't contain a main class.");
                        }

                        return mainClass.getAsString();
                    }
                }
            }

            throw new IllegalStateException("JAR does not contain a valid Velocity plugin.");
        }
    }

    private VelocityPluginDescription createDescription(Plugin annotation, Path source, Class mainClass) {
        Set<PluginDependency> dependencies = new HashSet<>();

        for (Dependency dependency : annotation.dependencies()) {
            dependencies.add(toDependencyMeta(dependency));
        }

        return new JavaVelocityPluginDescription(
                annotation.id(),
                annotation.version(),
                annotation.author(),
                dependencies,
                source,
                mainClass
        );
    }

    private static PluginDependency toDependencyMeta(Dependency dependency) {
        return new PluginDependency(
            dependency.id(),
            null, // TODO Implement version matching in dependency annotation
            dependency.optional()
        );
    }
}
