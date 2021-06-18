/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network.registry;

import com.velocitypowered.api.network.PlatformVersion;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProtocolVersionRegistry {
  /**
   * Returns a collection of supported protocol versions for the given {@code platform}. The
   * collection is expected to be ordered and immutable.
   *
   * @param platform the platform to look up the versions for
   * @return an ordered, immutable collection of supported protocol versions
   */
  Collection<PlatformVersion> supported(Platform platform);

  @Nullable PlatformVersion lookup(Platform platform, int version);
}
