package com.velocitypowered.proxy.protocol.packet.chat.legacy;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;

public class LegacyChatHandler implements ChatHandler<LegacyChat> {
  private final VelocityServer server;
  private final ConnectedPlayer player;

  public LegacyChatHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public Class<LegacyChat> packetClass() {
    return LegacyChat.class;
  }

  @Override
  public void handlePlayerChatInternal(LegacyChat packet) {
    MinecraftConnection serverConnection = player.ensureAndGetCurrentServer().ensureConnected();
    if (serverConnection == null) {
      return;
    }
    this.server.getEventManager().fire(new PlayerChatEvent(this.player, packet.getMessage()))
        .whenComplete((chatEvent, throwable) -> {
          if (chatEvent.getResult().isAllowed()) {
            return;
          }

          serverConnection.write(ChatBuilder.builder(this.player.getProtocolVersion())
              .message(chatEvent.getResult().getMessage().orElse(packet.getMessage())).toServer());
        });
  }
}
