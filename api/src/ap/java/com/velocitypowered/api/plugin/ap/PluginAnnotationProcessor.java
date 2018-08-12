package com.velocitypowered.api.plugin.ap;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginCandidate;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@SupportedAnnotationTypes({"com.velocitypowered.api.plugin.Plugin"})
public class PluginAnnotationProcessor extends AbstractProcessor {
    private ProcessingEnvironment environment;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.environment = processingEnv;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Plugin.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with "
                        + Plugin.class.getCanonicalName());
                return false;
            }

            Name qualifiedName = ((TypeElement) element).getQualifiedName();
            Plugin plugin = element.getAnnotation(Plugin.class);
            if (!PluginCandidate.ID_PATTERN.matcher(plugin.id()).matches()) {
                environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid ID for plugin "
                        + qualifiedName + ". IDs must start alphabetically, have alphanumeric characters, and can " +
                        "contain dashes or underscores.");
                return false;
            }

            // All good, generate the velocity-plugin.json.
            Map<String, Object> pluginJson = new HashMap<>();
            pluginJson.put("id", plugin.id());
            pluginJson.put("main", qualifiedName.toString());
            pluginJson.put("author", plugin.author());
            List<Map<String, Object>> serializedDependencies = new ArrayList<>();
            for (Dependency dependency : plugin.dependencies()) {
                serializedDependencies.add(ImmutableMap.of("id", dependency.id(), "optional", dependency.optional()));
            }
            pluginJson.put("dependencies", serializedDependencies);
            pluginJson.put("version", plugin.version());
            try {
                FileObject object = environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "velocity-plugin.json");
                try (Writer writer = new BufferedWriter(object.openWriter())) {
                    new Gson().toJson(pluginJson, writer);
                }
            } catch (IOException e) {
                environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to generate plugin file");
            }
        }

        return false;
    }
}
