/*
 * Copyright (C) 2021-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks an event as an event the proxy will wait on to completely fire (including any
 * {@link com.velocitypowered.api.event.EventTask}s) before continuing handling it. Annotated
 * classes are suitable candidates for using EventTasks for handling complex asynchronous
 * operations in a non-blocking matter.
 */
@Target(ElementType.TYPE)
@Documented
public @interface AwaitingEvent {

}
