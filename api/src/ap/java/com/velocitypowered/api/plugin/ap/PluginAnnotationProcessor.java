/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin.ap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Annotation processor for Velocity.
 */
@SupportedAnnotationTypes({PluginProcessingEnvironment.PLUGIN_CLASS_NAME})
public class PluginAnnotationProcessor extends AbstractProcessor {

  private PluginProcessingEnvironment<Element> pluginEnv;

  @Override
  public synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.pluginEnv = new JavaProcessingEnvironment(processingEnv);
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

    final TypeElement pluginAnnElement = this.processingEnv.getElementUtils()
        .getTypeElement(PluginProcessingEnvironment.PLUGIN_CLASS_NAME);
    if (pluginAnnElement == null) {
      this.processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Unable to find an element of type "
              + PluginProcessingEnvironment.PLUGIN_CLASS_NAME + " on classpath");
      return false;
    }

    for (final Element element : roundEnv.getElementsAnnotatedWith(pluginAnnElement)) {
      if (!this.pluginEnv.process(element)) {
        return false;
      }
    }

    return false;
  }

  static final class JavaProcessingEnvironment extends PluginProcessingEnvironment<Element> {
    private final Messager messager;
    private final Filer filer;
    private final Elements elements;

    JavaProcessingEnvironment(final ProcessingEnvironment env) {
      this.messager = env.getMessager();
      this.filer = env.getFiler();
      this.elements = env.getElementUtils();
    }

    @Override
    void logNotice(final String message, final Element element) {
      this.messager.printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    @Override
    void logWarning(final String message, final Element element) {
      this.messager.printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    @Override
    void logError(final String message, final Element element) {
      this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    @Override
    void logError(final String message, final Element element, final Exception ex) {
      this.messager.printMessage(Diagnostic.Kind.ERROR, message, element);
      final StringWriter writer = new StringWriter();
      ex.printStackTrace(new PrintWriter(writer));
      this.messager.printMessage(Diagnostic.Kind.ERROR, writer.getBuffer());

    }

    @Override
    boolean isClass(final Element element) {
      return element.getKind().isClass();
    }

    @Override
    String getQualifiedName(final Element element) {
      return ((TypeElement) element).getQualifiedName().toString();
    }

    @Override
    @Nullable AnnotationWrapper getPluginAnnotation(final Element element) {
      for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
        final Element typeElement = mirror.getAnnotationType().asElement();
        if (typeElement.getSimpleName().contentEquals("Plugin")
            && (typeElement.getKind() == ElementKind.ANNOTATION_TYPE)
            && ((TypeElement) typeElement).getQualifiedName().contentEquals(PLUGIN_CLASS_NAME)) {
          return new JavaAnnotationWrapper(this.elements, mirror);
        }
      }
      return null;
    }

    @Override
    BufferedWriter openWriter(
        final String pkg,
        final String name,
        final Element sourceElement
    ) throws IOException {
      final FileObject file = this.filer.createResource(
          StandardLocation.CLASS_OUTPUT, pkg, name, sourceElement);
      return new BufferedWriter(file.openWriter());
    }

    static final class JavaAnnotationWrapper implements AnnotationWrapper {
      private final Elements elements;
      private final Map<String, Object> explicitValues;
      private final Map<String, Object> defaultValues;

      JavaAnnotationWrapper(final Elements elements, final AnnotationMirror annotation) {
        final Map<String, Object> explicitVals = new HashMap<>();
        for (final var methodToValue : annotation.getElementValues().entrySet()) {
          final String name = methodToValue.getKey().getSimpleName().toString();
          final Object value = methodToValue.getValue().getValue();

          explicitVals.put(name, value);
        }

        final Map<String, Object> defaultVals = new HashMap<>();
        final var elementsWithDefaults = elements.getElementValuesWithDefaults(annotation);
        for (final var methodToValue : elementsWithDefaults.entrySet()) {
          final String name = methodToValue.getKey().getSimpleName().toString();
          if (explicitVals.containsKey(name)) {
            continue;
          }

          final Object value = methodToValue.getValue().getValue();
          defaultVals.put(name, value);
        }


        this.elements = elements;
        this.explicitValues = explicitVals;
        this.defaultValues = defaultVals;
      }

      private Object unbox(final Object boxed) {
        if (boxed instanceof TypeMirror) { // .class reference
          // TODO: not used for Velocity
          return boxed;
        } else if (boxed instanceof VariableElement) { // enum constant
          return ((VariableElement) boxed).getSimpleName();
        } else if (boxed instanceof AnnotationMirror) { // nested annotation
          return new JavaAnnotationWrapper(this.elements, (AnnotationMirror) boxed);
        } else if (boxed instanceof List<?>) {
          @SuppressWarnings("unchecked")
          final List<AnnotationValue> boxedList = (List<AnnotationValue>) boxed;
          final List<Object> val = new ArrayList<>(boxedList.size());
          for (final AnnotationValue o : boxedList) {
            val.add(this.unbox(o.getValue()));
          }
          return val;
        } else if (boxed instanceof CharSequence) {
          return boxed.toString();
        } else {
          return boxed;
        }
      }

      private <T> T unboxAndCast(final Object o, final Class<T> expectedType) {
        final Object value = this.unbox(o);
        if (!expectedType.isInstance(value)) {
          throw new IllegalArgumentException("Expected '" + value + "' to be a "
              + expectedType + ", but it was a " + value.getClass() + " instead");
        }
        return expectedType.cast(value);
      }

      @Override
      public <T> @Nullable T get(final String key, final Class<T> expectedType) {
        Object value = this.explicitValues.get(key);
        if (value == null) {
          value = this.defaultValues.get(key);
        }

        if (value == null) {
          return null;
        }

        return this.unboxAndCast(value, expectedType);
      }

      @Override
      public @Nullable <T> List<T> getList(final String key, final Class<T> expectedType) {
        Object value = this.explicitValues.get(key);
        if (value == null) {
          value = this.defaultValues.get(key);
        }

        if (value == null) {
          return null;
        }

        if (value instanceof List<?>) {
          @SuppressWarnings("unchecked")
          final List<AnnotationValue> boxedList = (List<AnnotationValue>) value;
          final List<T> val = new ArrayList<>(boxedList.size());
          for (final AnnotationValue o : boxedList) {
            val.add(this.unboxAndCast(o.getValue(), expectedType));
          }
          return val;
        } else {
          return Collections.singletonList(this.unboxAndCast(value, expectedType));
        }
      }

      @Override
      public boolean isExplicit(final String key) {
        return this.explicitValues.containsKey(key);
      }
    }
  }
}
