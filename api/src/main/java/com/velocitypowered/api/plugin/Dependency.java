/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the {@link Plugin} depends on another plugin in order to enable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Dependency {

  /**
   * The plugin ID of the dependency.
   *
   * @return The dependency plugin ID
   * @see Plugin#id()
   */
  String id();

  /**
   * The required version of the dependency. This should be in an NPM-compatible versioning format,
   * which you can figure from <a href="https://semver.npmjs.com/">npm's SemVer checker</a>. If
   * not specified, this assumes any version is acceptable.
   *
   * @return the version requirement
   */
  String version() default "*";

  /**
   * Whether or not the dependency is not required to enable this plugin. By default this is
   * {@code false}, meaning that the dependency is required to enable this plugin.
   *
   * @return true if the dependency is not required for the plugin to work
   */
  boolean optional() default false;
}
