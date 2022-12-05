package com.velocitypowered.proxy.protocol.packet.chat.signedv2;

import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;

public class ChatHandlerV2 implements ChatHandler<PlayerChatV2> {
  @Override
  public Class<PlayerChatV2> packetClass() {
    return PlayerChatV2.class;
  }

  @Override
  public void handlePlayerChat(PlayerChatV2 packet) {

  }
}
