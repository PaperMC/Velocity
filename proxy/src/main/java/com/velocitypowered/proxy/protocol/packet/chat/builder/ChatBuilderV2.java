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

package com.velocitypowered.proxy.protocol.packet.chat.builder;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatType;
import java.time.Instant;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class ChatBuilderV2 {
  protected final ProtocolVersion version;
  protected @MonotonicNonNull Component component;
  protected @MonotonicNonNull String message;
  protected @Nullable Player sender;
  protected @Nullable Identity senderIdentity;
  protected Instant timestamp;
  protected ChatType type = ChatType.CHAT;

  protected ChatBuilderV2(ProtocolVersion version) {
    this.version = version;
    this.timestamp = Instant.now();
  }

  public ChatBuilderV2 component(Component component) {
    this.component = component;
    return this;
  }

  public ChatBuilderV2 message(String message) {
    this.message = message;
    return this;
  }

  public ChatBuilderV2 setType(ChatType chatType) {
    this.type = chatType;
    return this;
  }

  public ChatBuilderV2 setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public ChatBuilderV2 forIdentity(Identity identity) {
    this.senderIdentity = identity;
    return this;
  }

  public ChatBuilderV2 asPlayer(@Nullable Player player) {
    this.sender = player;
    return this;
  }

  public ChatBuilderV2 asServer() {
    this.senderIdentity = null;
    return this;
  }

  public abstract MinecraftPacket toClient();

  public abstract MinecraftPacket toServer();
}
