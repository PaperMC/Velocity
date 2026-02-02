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

package com.velocitypowered.proxy.protocol.packet.chat.keyed;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatType;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.builder.ChatBuilderV2;
import net.kyori.adventure.text.Component;

/**
 * A concrete implementation of {@link ChatBuilderV2} that uses keys to build chat components.
 *
 * <p>The {@code KeyedChatBuilder} class extends the functionality of {@link ChatBuilderV2} by allowing
 * chat components to be built using specific keys, enabling dynamic message construction.</p>
 */
public class KeyedChatBuilder extends ChatBuilderV2 {

  public KeyedChatBuilder(ProtocolVersion version) {
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
    if (message.startsWith("/")) {
      return new KeyedPlayerCommandPacket(message.substring(1), ImmutableList.of(), timestamp);
    } else {
      // This will produce an error on the server, but needs to be here.
      KeyedPlayerChatPacket v1Chat = new KeyedPlayerChatPacket(message);
      v1Chat.setExpiry(this.timestamp);
      return v1Chat;
    }
  }
}
