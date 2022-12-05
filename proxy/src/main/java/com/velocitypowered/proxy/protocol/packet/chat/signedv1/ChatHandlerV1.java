package com.velocitypowered.proxy.protocol.packet.chat.signedv1;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatHandlerV1 implements ChatHandler<PlayerChatV1> {
  private static final Logger logger = LogManager.getLogger(ChatHandlerV1.class);
  private final VelocityServer server;
  private final ConnectedPlayer player;

  public ChatHandlerV1(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public Class<PlayerChatV1> packetClass() {
    return PlayerChatV1.class;
  }

  @Override
  public void handlePlayerChatInternal(PlayerChatV1 packet) {
    ChatQueue chatQueue = this.player.getChatQueue();
    EventManager eventManager = this.server.getEventManager();
    PlayerChatEvent toSend = new PlayerChatEvent(player, packet.getMessage());
    CompletableFuture<PlayerChatEvent> future = eventManager.fire(toSend);

    SignedMessage signedMessage;
    CompletableFuture<MinecraftPacket> chatFuture;
    IdentifiedKey playerKey = this.player.getIdentifiedKey();

    if (playerKey != null && (signedMessage = packet.isUnsigned()
        ? null
        : packet.signedContainer(this.player.getIdentifiedKey(), this.player.getUniqueId(), false)) != null) {
      chatFuture = future.thenApply(pme -> {
        PlayerChatEvent.ChatResult chatResult = pme.getResult();
        if (!chatResult.isAllowed() && playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
          logger.fatal("A plugin tried to cancel a signed chat message."
              + " This is no longer possible in 1.19.1 and newer. "
              + "Disconnecting player " + player.getUsername());
          player.disconnect(Component.text("A proxy plugin caused an illegal protocol state. "
              + "Contact your network administrator."));
          return null;
        }

        if (chatResult.getMessage().isPresent()) {
          String messageNew = chatResult.getMessage().get();
          if (!messageNew.equals(signedMessage.getMessage())) {
            if (playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
              // Bad, very bad.
              logger.fatal("A plugin tried to change a signed chat message. "
                  + "This is no longer possible in 1.19.1 and newer. "
                  + "Disconnecting player " + player.getUsername());
              player.disconnect(Component.text("A proxy plugin caused an illegal protocol state. "
                  + "Contact your network administrator."));
            } else {
              logger.warn("A plugin changed a signed chat message. The server may not accept it.");
              return ChatBuilder.builder(player.getProtocolVersion())
                  .message(messageNew).toServer();
            }
          }
        }
        return packet;
      });
    } else {
      chatFuture = future.thenApply(pme -> {
        PlayerChatEvent.ChatResult chatResult = pme.getResult();
        if (!chatResult.isAllowed()) {
          return null;
        }

        return ChatBuilder.builder(this.player.getProtocolVersion())
            .message(chatResult.getMessage().orElse(packet.getMessage())).toServer();
      });
    }
    chatQueue.queuePacket(
        chatFuture.exceptionally((ex) -> {
          logger.error("Exception while handling player chat for {}", player, ex);
          return null;
        }),
        packet.getExpiry()
    );
  }
}
