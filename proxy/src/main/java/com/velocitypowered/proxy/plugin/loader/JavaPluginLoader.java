package com.velocitypowered.proxy.plugin.loader;

import com.velocitypowered.api.plugin.*;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import com.velocitypowered.proxy.plugin.loader.java.JavaVelocityPluginCandidate;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class JavaPluginLoader implements PluginLoader {
    private final ProxyServer server;

    public JavaPluginLoader(ProxyServer server) {
        this.server = server;
    }

    @Nonnull
    @Override
    public PluginCandidate loadPlugin(Path source) throws Exception {
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

        VelocityPluginCandidate description = createDescription((Plugin) annotation, source, mainClass);

        String pluginId = description.getId();
        Pattern pattern = PluginCandidate.ID_PATTERN;

        if (!pattern.matcher(pluginId).matches()) {
            throw new InvalidPluginException("Plugin ID '" + pluginId + "' must match pattern " + pattern.pattern());
        }

        return description;
    }

    @Nonnull
    @Override
    public PluginContainer createPlugin(PluginCandidate description) throws Exception {
        if (!(description instanceof JavaVelocityPluginCandidate)) {
            throw new IllegalArgumentException("Description provided isn't of the Java plugin loader");
        }

        JavaVelocityPluginCandidate javaDescription = (JavaVelocityPluginCandidate) description;
        Optional<Path> source = javaDescription.getSource();

        if (!source.isPresent()) {
            throw new IllegalArgumentException("No path in plugin description");
        }

        Object instance = javaDescription.getMainClass().newInstance();
        // TODO Inject server variable, logger...
        // Should we add Guice?

        return new VelocityPluginContainer(
            description.getId(),
            description.getVersion().orElse(null),
            description.getAuthor().orElse(null),
            description.getDependencies(),
            source.get(),
            instance
        );
    }

    private String getMainClassName(Path source) throws Exception {
        try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            Manifest manifest = in.getManifest();

            if (manifest == null) {
                throw new InvalidPluginException("Jar does not contain a manifest");
            }

            Attributes attributes = manifest.getMainAttributes();
            String className = attributes.getValue("Main-Class");

            if (className == null) {
                throw new InvalidPluginException("Plugin manifest does not contain Main-Class attribute");
            }

            return className;
        }
    }

    private VelocityPluginCandidate createDescription(Plugin annotation, Path source, Class mainClass) {
        Set<PluginDependency> dependencies = new HashSet<>();

        for (Dependency dependency : annotation.dependencies()) {
            dependencies.add(toDependencyMeta(dependency));
        }

        return new JavaVelocityPluginCandidate(
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
