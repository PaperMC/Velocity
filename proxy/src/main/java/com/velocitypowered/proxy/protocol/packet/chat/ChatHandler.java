package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.protocol.MinecraftPacket;

public interface ChatHandler<T extends MinecraftPacket> {
  Class<T> packetClass();

  void handlePlayerChat(T packet);
}
