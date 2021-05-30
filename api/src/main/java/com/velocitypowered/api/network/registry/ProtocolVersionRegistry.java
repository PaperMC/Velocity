package com.velocitypowered.api.network.registry;

import com.velocitypowered.api.network.PlatformVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProtocolVersionRegistry {
  @Nullable PlatformVersion lookup(Platform platform, int version);
}
