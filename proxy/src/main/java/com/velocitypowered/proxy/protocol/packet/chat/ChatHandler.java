package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.protocol.MinecraftPacket;

public interface ChatHandler<T extends MinecraftPacket> {
  Class<T> packetClass();

  void handlePlayerChatInternal(T packet);

  default boolean handlePlayerChat(MinecraftPacket packet) {
    if (packetClass().isInstance(packet)) {
      handlePlayerChatInternal(packetClass().cast(packet));
      return true;
    }
    return false;
  }
}
