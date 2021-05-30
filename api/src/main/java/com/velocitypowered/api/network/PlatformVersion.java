package com.velocitypowered.api.network;

import com.velocitypowered.api.network.registry.Platform;
import java.util.List;

public interface PlatformVersion {
  Platform platform();

  int protocolVersion();

  List<String> supportedVersions();
}
