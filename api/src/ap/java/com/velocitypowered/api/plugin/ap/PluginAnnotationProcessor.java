package com.velocitypowered.api.plugin.ap;

import com.google.gson.Gson;
import com.velocitypowered.api.plugin.Plugin;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;
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

@SupportedAnnotationTypes({"com.velocitypowered.api.plugin.Plugin"})
public class PluginAnnotationProcessor extends AbstractProcessor {

  private ProcessingEnvironment environment;
  private String pluginClassFound;
  private boolean warnedAboutMultiplePlugins;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    this.environment = processingEnv;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(Plugin.class)) {
      if (element.getKind() != ElementKind.CLASS) {
        environment.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with "
                + Plugin.class.getCanonicalName());
        return false;
      }

      Name qualifiedName = ((TypeElement) element).getQualifiedName();

      if (Objects.equals(pluginClassFound, qualifiedName.toString())) {
        if (!warnedAboutMultiplePlugins) {
          environment.getMessager()
              .printMessage(Diagnostic.Kind.WARNING, "Velocity does not yet currently support "
                  + "multiple plugins. We are using " + pluginClassFound
                  + " for your plugin's main class.");
          warnedAboutMultiplePlugins = true;
        }
        return false;
      }

      Plugin plugin = element.getAnnotation(Plugin.class);
      if (!SerializedPluginDescription.ID_PATTERN.matcher(plugin.id()).matches()) {
        environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Invalid ID for plugin "
            + qualifiedName
            + ". IDs must start alphabetically,be lowercase, have alphanumeric characters, and can "
            + "contain dashes or underscores.");
        return false;
      }

      // All good, generate the velocity-plugin.json.
      SerializedPluginDescription description = SerializedPluginDescription
          .from(plugin, qualifiedName.toString());
      try {
        FileObject object = environment.getFiler()
            .createResource(StandardLocation.CLASS_OUTPUT, "", "velocity-plugin.json");
        try (Writer writer = new BufferedWriter(object.openWriter())) {
          new Gson().toJson(description, writer);
        }
        pluginClassFound = qualifiedName.toString();
      } catch (IOException e) {
        environment.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, "Unable to generate plugin file");
      }
    }

    return false;
  }
}
