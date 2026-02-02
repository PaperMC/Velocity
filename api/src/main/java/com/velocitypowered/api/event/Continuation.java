/*
 * Copyright (C) 2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Represents a continuation of a paused event handler. Any of the resume methods
 * may only be called once otherwise an {@link IllegalStateException} is expected.
 */
public interface Continuation {

  /**
   * Resumes the continuation.
   */
  void resume();

  /**
   * Resumes the continuation after the executed task failed.
   *
   * @param exception the {@link Throwable} that caused the failure
   */
  void resumeWithException(Throwable exception);
}
