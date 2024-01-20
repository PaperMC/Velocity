/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin.ap;

import com.google.gson.Gson;
import com.sun.source.util.Plugin;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Shared logic between different processing platforms.
 *
 * @param <E> the native element type
 */
abstract class PluginProcessingEnvironment<E> {
  static final String PLUGIN_CLASS_NAME = "com.velocitypowered.api.plugin.Plugin";
  static final String META_FILE_LOCATION = "velocity-plugin.json";

  // State
  private String pluginClassFound;
  private boolean warnedAboutMultiplePlugins;

  // Implementation-specific hooks

  abstract void logNotice(final String message, final E element);

  abstract void logWarning(final String message, final E element);

  abstract void logError(final String message, final E element);

  abstract void logError(final String message, final E element, final Exception ex);

  abstract boolean isClass(final E element);

  abstract String getQualifiedName(final E element);

  abstract AnnotationWrapper getPluginAnnotation(final E element);

  abstract BufferedWriter openWriter(
      final String pkg, final String name, final E sourceElement) throws IOException;

  // Shared processing logic

  /**
   * Process a single element annotated with the
   * {@code com.velocitypowered.api.plugin.Plugin} annotation.
   *
   * @param element the element
   * @return whether further elements should be processed
   */
  boolean process(final E element) {
    if (!this.preValidate(element)) {
      return false;
    }

    final String qualifiedName = this.getQualifiedName(element);
    if (this.generate(element, qualifiedName)) {
      this.pluginClassFound = qualifiedName;
      return true;
    }

    return false;
  }

  // Validation before parsing full annotation
  private boolean preValidate(final E element) {
    if (!this.isClass(element)) {
      this.logError("Only classes can be annotated with "
          + Plugin.class.getCanonicalName(), element);
      return false;
    }

    final String qualifiedName = this.getQualifiedName(element);

    if (Objects.equals(this.pluginClassFound, qualifiedName)) {
      if (!this.warnedAboutMultiplePlugins) {
        this.logWarning("Velocity does not yet currently support "
            + "multiple plugins. We are using " + this.pluginClassFound
            + " for your plugin's main class.", element);
        this.warnedAboutMultiplePlugins = true;
      }
      return false;
    }

    return true;
  }


  // Validation and generation after fetching full annotation
  private boolean generate(final E element, final String qualifiedName) {
    final AnnotationWrapper plugin = this.getPluginAnnotation(element);
    final String id = plugin.get(SerializedPluginDescription.PLUGIN_ID, String.class);
    if (id == null || !SerializedPluginDescription.ID_PATTERN.matcher(id).matches()) {
      this.logError("Invalid ID '" + id + "'for plugin "
          + ". IDs must start alphabetically, have lowercase alphanumeric characters, and "
          + "can contain dashes or underscores.", element);
      return false;
    }

    // All good, generate the velocity-plugin.json.
    final SerializedPluginDescription description = SerializedPluginDescription
        .from(plugin, qualifiedName);
    try (final var writer = this.openWriter("", META_FILE_LOCATION, element)) {
      new Gson().toJson(description, writer);
    } catch (final IOException e) {
      this.logError("Unable to generate plugin file", element, e);
    }

    return true;
  }

  /**
   * A wrapper around platform annotation types.
   *
   * <p>Values are modeled similar to the jx.processing api, with several simplifications</p>
   * <ul>
   *   <li>Primitives as their wrappers</li>
   *   <li>Strings as Strings</li>
   *   <li>Classes as their qualified name (a String)</li>
   *   <li></li>
   *   <li>Lists/arrays have a special getter method (though they are exposed as lists)</li>
   *   <li>Sub-annotations are exposed as annotation wrappers</li>
   * </ul>
   *
   * <p>Unboxing of values may be done lazily, as desired by the implementation</p>
   */
  interface AnnotationWrapper {
    /**
     * Get a value of a certain type.
     *
     * @param key the key
     * @param expectedType the expected unboxed types
     * @return a value, if any is present
     * @param <T> the value type
     */
    <T> @Nullable T get(final String key, final Class<T> expectedType);

    /**
     * Get a value of a certain type.
     *
     * @param key the key
     * @param expectedType the expected unboxed types
     * @return a list of values
     * @param <T> the value type
     */
    <T> @Nullable List<T> getList(final String key, final Class<T> expectedType);

    /**
     * Get if a value has been provided explicitly, rather than through a default.
     *
     * @param key the annotation field
     * @return whether this value is explicit
     */
    boolean isExplicit(final String key);
  }

}
