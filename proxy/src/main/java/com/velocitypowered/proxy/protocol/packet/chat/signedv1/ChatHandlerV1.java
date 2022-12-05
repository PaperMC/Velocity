package com.velocitypowered.proxy.protocol.packet.chat.signedv1;

import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;

public class ChatHandlerV1 implements ChatHandler<PlayerChatV1> {
  @Override
  public Class<PlayerChatV1> packetClass() {
    return PlayerChatV1.class;
  }

  @Override
  public void handlePlayerChat(PlayerChatV1 packet) {

  }
}
