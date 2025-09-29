/*
 * Copyright (C) 2018-2021 Velocity Contributors
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
   * The order events will be posted to this listener.
   *
   * @deprecated specify the order using {@link #priority()} instead
   * @return the order
   */
  @Deprecated
  PostOrder order() default PostOrder.NORMAL;

  /**
   * The priority of this event handler. Priorities are used to determine the order in which event
   * handlers are called. The higher the priority, the earlier the event handler will be called.
   *
   * @return the priority
   */
  short priority() default 0;

  /**
   * Whether the handler must be called asynchronously. By default, all event handlers are called
   * asynchronously.
   *
   * <p>For performance (for instance, if you use {@link EventTask#withContinuation}), you can
   * optionally specify <code>false</code>. This option will become {@code false} by default
   * in a future release of Velocity.</p>
   *
   * <p>If this is {@code true}, the method is guaranteed to be executed asynchronously. Otherwise,
   * the handler may be executed on the current thread or asynchronously. <strong>This still means
   * you must consider thread-safety in your event listeners</strong> as the "current thread" can
   * and will be different each time.</p>
   *
   * <p>Note that if any method handler targeting an event type is marked with {@code true}, then
   * every handler targeting that event type will be executed asynchronously.</p>
   *
   * @return Requires async
   */
  boolean async() default true;

}
