package com.velocitypowered.api.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a dependency for a {@link Plugin}
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

    // TODO Add required version field

    /**
     * If this dependency is optional for the plugin to work. By default
     * this is {@code false}.
     *
     * @return true if the dependency is optional for the plugin to work
     */
    boolean optional() default false;
}
