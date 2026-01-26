/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat.session;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatType;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.LastSeenMessages;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.builder.ChatBuilderV2;
import net.kyori.adventure.text.Component;

/**
 * A concrete implementation of {@link ChatBuilderV2} for handling session-based chat messages.
 *
 * <p>The {@code SessionChatBuilder} is designed to build chat components that are specific to
 * a player's session, allowing customization and context-specific formatting of chat messages
 * within the current session.</p>
 */
public class SessionChatBuilder extends ChatBuilderV2 {

  public SessionChatBuilder(ProtocolVersion version) {
    super(version);
  }

  @Override
  public MinecraftPacket toClient() {
    // This is temporary
    Component msg = component == null ? Component.text(message) : component;
    return new SystemChatPacket(new ComponentHolder(version, msg), type == ChatType.CHAT ? ChatType.SYSTEM : type);
  }

  @Override
  public MinecraftPacket toServer() {
    LastSeenMessages lastSeenMessages = this.lastSeenMessages != null ? this.lastSeenMessages : new LastSeenMessages();
    if (message.startsWith("/")) {
      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
        UnsignedPlayerCommandPacket command = new UnsignedPlayerCommandPacket();
        command.command = message.substring(1);
        return command;
      } else {
        SessionPlayerCommandPacket command = new SessionPlayerCommandPacket();
        command.command = message.substring(1);
        command.salt = 0L;
        command.timeStamp = timestamp;
        command.argumentSignatures = new SessionPlayerCommandPacket.ArgumentSignatures();
        command.lastSeenMessages = lastSeenMessages;
        return command;
      }
    } else {
      SessionPlayerChatPacket chat = new SessionPlayerChatPacket();
      chat.message = message;
      chat.signed = false;
      chat.signature = new byte[0];
      chat.timestamp = timestamp;
      chat.salt = 0L;
      chat.lastSeenMessages = lastSeenMessages;
      return chat;
    }
  }
}
