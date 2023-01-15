/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

/**
 * Represents the order an event will be posted to a listener method, relative to other listeners.
 */
public enum PostOrder {

  FIRST, EARLY, NORMAL, LATE, LAST

}
