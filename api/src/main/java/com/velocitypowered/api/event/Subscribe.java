/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates that this method can be used to listen for an event from the proxy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

  /**
   * The order events will be posted to this handler.
   *
   * @return the order
   */
  short order() default PostOrder.NORMAL;

  /**
   * Whether the handler must be called asynchronously.
   *
   * <p>If this method returns {@code true}, the method is guaranteed to be executed
   * asynchronously. Otherwise, the handler may be executed on the current thread or
   * asynchronously. <strong>This still means you must consider thread-safety in your
   * event listeners</strong> as the "current thread" can and will be different each time.</p>
   *
   * <p>If any method handler targeting an event type is marked with {@code true}, then every
   * handler targeting that event type will be executed asynchronously.</p>
   *
   * @return Requires async
   */
  boolean async() default false;
}
