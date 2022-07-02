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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.crypto.SignedChatCommand;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ChatBuilder {

  private final ProtocolVersion version;

  private @MonotonicNonNull Component component;
  private @MonotonicNonNull String message;
  private @MonotonicNonNull SignedChatMessage signedChatMessage;
  private @MonotonicNonNull SignedChatCommand signedCommand;

  private @Nullable Player sender;
  private @Nullable Identity senderIdentity;

  private ChatType type = ChatType.CHAT;

  private ChatBuilder(ProtocolVersion version) {
    this.version = version;
  }

  public static ChatBuilder builder(ProtocolVersion version) {
    return new ChatBuilder(Preconditions.checkNotNull(version));
  }

  public ChatBuilder component(Component message) {
    this.component = Preconditions.checkNotNull(message);
    return this;
  }

  /**
   * Sets the message to the provided message.
   *
   * @param message The message for the chat.
   * @return {@code this}
   */
  public ChatBuilder message(String message) {
    Preconditions.checkArgument(this.message == null);
    this.message = Preconditions.checkNotNull(message);
    return this;
  }

  /**
   * Sets the signed message to the provided message.
   *
   * @param message The signed message for the chat.
   * @return {@code this}
   */
  public ChatBuilder message(SignedChatMessage message) {
    Preconditions.checkNotNull(message);
    Preconditions.checkArgument(this.message == null);
    this.message = message.getMessage();
    this.signedChatMessage = message;
    return this;
  }

  /**
   * Sets the signed command to the provided command.
   *
   * @param command The signed command for the chat.
   * @return {@code this}
   */
  public ChatBuilder message(SignedChatCommand command) {
    Preconditions.checkNotNull(command);
    Preconditions.checkArgument(this.message == null);
    this.message = command.getBaseCommand();
    this.signedCommand = command;
    return this;
  }


  public ChatBuilder setType(ChatType type) {
    this.type = type;
    return this;
  }

  public ChatBuilder asPlayer(@Nullable Player player) {
    this.sender = player;
    return this;
  }

  public ChatBuilder forIdentity(@Nullable Identity identity) {
    this.senderIdentity = identity;
    return this;
  }

  public ChatBuilder asServer() {
    this.sender = null;
    return this;
  }

  /**
   * Creates a {@link MinecraftPacket} which can be sent to the client; using the provided information in the builder.
   *
   * @return The {@link MinecraftPacket} to send to the client.
   */
  public MinecraftPacket toClient() {
    // This is temporary
    UUID identity = sender == null ? (senderIdentity == null ? Identity.nil().uuid()
        : senderIdentity.uuid()) : sender.getUniqueId();
    Component msg = component == null ? Component.text(message) : component;

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      // hard override chat > system for now
      return new SystemChat(msg, type == ChatType.CHAT ? ChatType.SYSTEM.getId() : type.getId());
    } else {
      return new LegacyChat(ProtocolUtils.getJsonChatSerializer(version).serialize(msg), type.getId(), identity);
    }
  }

  /**
   * Creates a {@link MinecraftPacket} which can be sent to the server; using the provided information in the builder.
   *
   * @return The {@link MinecraftPacket} to send to the server.
   */
  public MinecraftPacket toServer() {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (signedChatMessage != null) {
        return new PlayerChat(signedChatMessage);
      } else if (signedCommand != null) {
        return new PlayerCommand(signedCommand);
      } else {
        // Well crap
        if (message.startsWith("/")) {
          return new PlayerCommand(message.substring(1), ImmutableList.of(), Instant.now());
        } else {
          // This will produce an error on the server, but needs to be here.
          return new PlayerChat(message);
        }
      }
    }
    LegacyChat chat = new LegacyChat();
    chat.setMessage(message);
    return chat;
  }

  public static enum ChatType {
    CHAT((byte) 0),
    SYSTEM((byte) 1),
    GAME_INFO((byte) 2);

    private final byte raw;

    ChatType(byte raw) {
      this.raw = raw;
    }

    public byte getId() {
      return raw;
    }
  }
}
