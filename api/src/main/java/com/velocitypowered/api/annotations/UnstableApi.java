package com.velocitypowered.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks unstable API interfaces that are still maturing. These interfaces may change drastically
 * between minor releases of Velocity, and it is not guaranteed that the APIs marked with this
 * annotation will be stable over time.
 */
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.PACKAGE })
public @interface UnstableApi {

}
