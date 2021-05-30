/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network.registry;

import com.velocitypowered.api.network.PlatformVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProtocolVersionRegistry {
  @Nullable PlatformVersion lookup(Platform platform, int version);
}
