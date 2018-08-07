package com.velocitypowered.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to describe a Velocity plugin.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    /**
     * The ID of the plugin. This ID should be unique as to
     * not conflict with other plugins.
     *
     * The plugin ID must match the {@link PluginCandidate#ID_PATTERN}.
     *
     * @return the ID for this plugin
     */
    String id();

    /**
     * The version of the plugin.
     *
     * @return the version of the plugin, or an empty string if unknown
     */
    String version() default "";

    /**
     * The author of the plugin.
     *
     * @return the plugin's author, or empty if unknown
     */
    String author() default "";

    /**
     * The dependencies required to load before this plugin.
     *
     * @return the plugin dependencies
     */
    Dependency[] dependencies() default {};
}
