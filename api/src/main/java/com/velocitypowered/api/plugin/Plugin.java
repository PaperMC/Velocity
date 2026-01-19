/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

import com.velocitypowered.api.plugin.ap.SerializedPluginDescription;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Pattern;

/**
 * Annotation used to describe a Velocity plugin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

  /**
   * The ID of the plugin. This ID should be unique as to not conflict with other plugins. The
   * plugin ID may contain alphanumeric characters, dashes, and underscores, and be a maximum
   * of 64 characters long.
   *
   * @return the ID for this plugin
   */
  @Pattern(SerializedPluginDescription.ID_PATTERN_STRING)
  String id();

  /**
   * The human readable name of the plugin as to be used in descriptions and similar things.
   *
   * @return The plugin name, or an empty string if unknown
   */
  String name() default "";

  /**
   * The version of the plugin.
   *
   * @return the version of the plugin, or an empty string if unknown
   */
  String version() default "";

  /**
   * The description of the plugin, explaining what it can be used for.
   *
   * @return The plugin description, or an empty string if unknown
   */
  String description() default "";

  /**
   * The URL or website of the plugin.
   *
   * @return The plugin url, or an empty string if unknown
   */
  String url() default "";

  /**
   * The author of the plugin.
   *
   * @return the plugin's author, or empty if unknown
   */
  String[] authors() default "";

  /**
   * The dependencies required to load before this plugin.
   *
   * @return the plugin dependencies
   */
  Dependency[] dependencies() default {};
}
