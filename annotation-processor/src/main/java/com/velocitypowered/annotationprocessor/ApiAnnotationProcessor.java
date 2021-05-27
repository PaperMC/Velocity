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

package com.velocitypowered.annotationprocessor;

import static com.velocitypowered.annotationprocessor.AnnotationProcessorConstants.EVENT_INTERFACE;
import static com.velocitypowered.annotationprocessor.AnnotationProcessorConstants.PLUGIN_ANNOTATION_CLASS;
import static com.velocitypowered.annotationprocessor.AnnotationProcessorConstants.SUBSCRIBE_ANNOTATION_CLASS;

import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes({PLUGIN_ANNOTATION_CLASS, SUBSCRIBE_ANNOTATION_CLASS})
public class ApiAnnotationProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    if (ProcessorUtils.contains(annotations, Subscribe.class)) {
      for (final Element e : roundEnv.getElementsAnnotatedWith(Subscribe.class)) {
        if (e.getKind() != ElementKind.METHOD) {
          this.processingEnv.getMessager().printMessage(
              Kind.ERROR, "Invalid element of type " + e.getKind()
                  + " annotated with @Subscribe", e);
          continue;
        }
        final ExecutableElement method = (ExecutableElement) e;

        final Messager msg = this.processingEnv.getMessager();
        if (method.getModifiers().contains(Modifier.STATIC)) {
          msg.printMessage(Diagnostic.Kind.ERROR, "method must not be static", method);
        }
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
          msg.printMessage(Diagnostic.Kind.ERROR, "method must be public", method);
        }
        if (method.getModifiers().contains(Modifier.ABSTRACT)) {
          msg.printMessage(Diagnostic.Kind.ERROR,
              "method must not be abstract", method);
        }
        Element enclosing = method.getEnclosingElement();
        if (enclosing != null && enclosing.getKind().isInterface()) {
          msg.printMessage(Diagnostic.Kind.ERROR,
              "interfaces cannot declare listeners", method);
        }
        final List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.isEmpty()
            || !this.isTypeSubclass(parameters.get(0), EVENT_INTERFACE)) {
          msg.printMessage(Diagnostic.Kind.ERROR,
              "method must have an Event as its first parameter", method);
        }
      }
    }

    if (ProcessorUtils.contains(annotations, Plugin.class)) {
      List<SerializedPluginDescription> found = new ArrayList<>();
      for (Element element : roundEnv.getElementsAnnotatedWith(Plugin.class)) {
        if (element.getKind() != ElementKind.CLASS) {
          processingEnv.getMessager()
              .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated with "
                  + Plugin.class.getCanonicalName());
          return false;
        }

        Name qualifiedName = ((TypeElement) element).getQualifiedName();

        Plugin plugin = element.getAnnotation(Plugin.class);
        if (!SerializedPluginDescription.ID_PATTERN.matcher(plugin.id()).matches()) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
              "Invalid ID for plugin " + qualifiedName + ". "
                  + "IDs must start alphabetically, have alphanumeric characters, and can "
                  + "contain dashes or underscores.");
          return false;
        }

        // All good, generate the velocity-plugin-info.json.
        SerializedPluginDescription description = SerializedPluginDescription
            .from(plugin, qualifiedName.toString());
        found.add(description);
      }

      try {
        FileObject object = processingEnv.getFiler()
            .createResource(StandardLocation.CLASS_OUTPUT, "", "velocity-plugin-info.json");
        try (Writer writer = new BufferedWriter(object.openWriter())) {
          new Gson().toJson(found, writer);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "Wrote velocity-plugin-info.json to " + object.toUri().toString());
      } catch (IOException e) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR, "Unable to generate plugin file");
      }
    }

    return false;
  }

  private boolean isTypeSubclass(final Element typedElement, final String subclass) {
    return isTypeSubclass(typedElement.asType(), subclass);
  }

  private boolean isTypeSubclass(final TypeMirror typeMirror, final String subclass) {
    final Elements elements = this.processingEnv.getElementUtils();
    final Types types = this.processingEnv.getTypeUtils();

    final TypeMirror event = types.getDeclaredType(elements.getTypeElement(subclass));
    return types.isAssignable(typeMirror, event);
  }
}
