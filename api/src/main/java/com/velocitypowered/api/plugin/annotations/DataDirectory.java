package com.velocitypowered.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation requests that Velocity inject a {@link java.nio.file.Path} instance with a data directory for the
 * plugin.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataDirectory {
}
