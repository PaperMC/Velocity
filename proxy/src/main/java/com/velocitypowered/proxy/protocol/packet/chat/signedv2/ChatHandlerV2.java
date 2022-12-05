package com.velocitypowered.proxy.protocol.packet.chat.signedv2;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ChatHandlerV2 implements ChatHandler<PlayerChatV2> {
  private final VelocityServer server;
  private final ConnectedPlayer player;
  private final @Nullable RemoteChatSession chatSession;

  public ChatHandlerV2(VelocityServer server, ConnectedPlayer player, @Nullable RemoteChatSession chatSession) {
    this.server = server;
    this.player = player;
    this.chatSession = chatSession;
  }

  @Override
  public Class<PlayerChatV2> packetClass() {
    return PlayerChatV2.class;
  }

  @Override
  public void handlePlayerChatInternal(PlayerChatV2 packet) {

  }
}
