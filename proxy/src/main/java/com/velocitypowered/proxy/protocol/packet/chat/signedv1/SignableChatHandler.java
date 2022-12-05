/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet.chat.signedv1;

import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignableChatHandler implements com.velocitypowered.proxy.protocol.packet.chat.ChatHandler<PlayerChat> {
  private static final Logger logger = LogManager.getLogger(SignableChatHandler.class);

  private final VelocityServer server;
  private final ConnectedPlayer player;

  public SignableChatHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public Class<PlayerChat> packetClass() {
    return PlayerChat.class;
  }

  private void invalidCancel() {
    logger.fatal("A plugin tried to cancel a signed chat message."
        + " This is no longer possible in 1.19.1 and newer. "
        + "Disconnecting player " + player.getUsername());
    player.disconnect(Component.text("A proxy plugin caused an illegal protocol state. "
        + "Contact your network administrator."));
  }

  private void invalidChange() {
    logger.fatal("A plugin tried to change a signed chat message. "
        + "This is no longer possible in 1.19.1 and newer. "
        + "Disconnecting player " + player.getUsername());
    player.disconnect(Component.text("A proxy plugin caused an illegal protocol state. "
        + "Contact your network administrator."));
  }

  @Override
  public void handlePlayerChatInternal(PlayerChat packet) {
    ChatQueue chatQueue = this.player.getChatQueue();
    EventManager eventManager = this.server.getEventManager();
    PlayerChatEvent toSend = new PlayerChatEvent(player, packet.getMessage());
    CompletableFuture<PlayerChatEvent> future = eventManager.fire(toSend);

    SignedMessage signedMessage = packet.isUnsigned()
        ? null
        : packet.signedContainer(this.player.getIdentifiedKey(), this.player.getUniqueId(), false);
    CompletableFuture<MinecraftPacket> chatFuture;
    IdentifiedKey playerKey = this.player.getIdentifiedKey();

    if (this.player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      // latest chat
      chatFuture = future.thenApply(handleChat(packet, packet.isUnsigned()));
    } else if (playerKey != null && signedMessage != null) {
      // 1.19->1.19.2 signed version
      chatFuture = future.thenApply(handleOldSignedChat(packet, signedMessage));
    } else {
      // 1.19->1.19.2 unsigned version
      chatFuture = future.thenApply(handleOldUnsignedChat(packet));
    }
    chatQueue.queuePacket(
        chatFuture.exceptionally((ex) -> {
          logger.error("Exception while handling player chat for {}", player, ex);
          return null;
        }),
        packet.getExpiry()
    );
  }

  private Function<PlayerChatEvent, MinecraftPacket> handleChat(PlayerChat packet, boolean unsigned) {
    return pme -> {
      PlayerChatEvent.ChatResult chatResult = pme.getResult();
      if (!chatResult.isAllowed()) {
        if (!unsigned) {
          invalidCancel();
        }
        return null;
      }

      if (chatResult.getMessage().map(str -> !str.equals(packet.getMessage())).orElse(false)) {
        invalidChange();
      }
      return packet;
    };
  }

  private Function<PlayerChatEvent, MinecraftPacket> handleOldSignedChat(PlayerChat packet, SignedMessage signed) {
    IdentifiedKey playerKey = this.player.getIdentifiedKey();
    assert playerKey != null;
    return pme -> {
      PlayerChatEvent.ChatResult chatResult = pme.getResult();
      if (!chatResult.isAllowed() && playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
        invalidCancel();
        return null;
      }

      if (chatResult.getMessage().map(str -> !str.equals(packet.getMessage())).orElse(false)) {
        if (playerKey.getKeyRevision().compareTo(IdentifiedKey.Revision.LINKED_V2) >= 0) {
          // Bad, very bad.
          invalidChange();
        } else {
          logger.warn("A plugin changed a signed chat message. The server may not accept it.");
          return ChatBuilder.builder(player.getProtocolVersion())
              .message(chatResult.getMessage().get() /* always present at this point */)
              .timestamp(packet.getExpiry())
              .toServer();
        }
      }
      return packet;
    };
  }

  private Function<PlayerChatEvent, MinecraftPacket> handleOldUnsignedChat(PlayerChat packet) {
    return pme -> {
      PlayerChatEvent.ChatResult chatResult = pme.getResult();
      if (!chatResult.isAllowed()) {
        return null;
      }

      return ChatBuilder.builder(this.player.getProtocolVersion())
          .message(chatResult.getMessage().orElse(packet.getMessage())).timestamp(packet.getExpiry()).toServer();
    };
  }
}
