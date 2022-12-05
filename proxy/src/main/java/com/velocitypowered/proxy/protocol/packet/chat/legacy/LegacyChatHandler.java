package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;

public class LegacyChatHandler implements ChatHandler<LegacyChat> {

  @Override
  public Class<LegacyChat> packetClass() {
    return LegacyChat.class;
  }

  @Override
  public void handlePlayerChat(LegacyChat packet) {

  }
}
