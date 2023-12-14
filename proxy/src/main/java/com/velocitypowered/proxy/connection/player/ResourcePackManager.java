package com.velocitypowered.proxy.connection.player;

import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;

public interface ResourcePackManager {

  void processResponse(ResourcePackResponse response);

  void sendRequest(ResourcePackRequest request);
}
