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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Represents a packet sent between the server and client to handle chat completion suggestions.
 * This packet allows the server to send chat message completions or suggestions to the client,
 * helping users complete commands or chat messages.
 */
public class PlayerChatCompletionPacket implements MinecraftPacket {

  private String[] completions;
  private Action action;

  public PlayerChatCompletionPacket() {
  }

  public PlayerChatCompletionPacket(String[] completions, Action action) {
    this.completions = completions;
    this.action = action;
  }

  public String[] getCompletions() {
    return completions;
  }

  public Action getAction() {
    return action;
  }

  public void setCompletions(String[] completions) {
    this.completions = completions;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    action = Action.values()[ProtocolUtils.readVarInt(buf)];
    completions = ProtocolUtils.readStringArray(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, action.ordinal());
    ProtocolUtils.writeStringArray(buf, completions);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  /**
   * Represents the different actions that can be taken with chat completions.
   */
  public enum Action {
    ADD,
    REMOVE,
    SET
  }
}
