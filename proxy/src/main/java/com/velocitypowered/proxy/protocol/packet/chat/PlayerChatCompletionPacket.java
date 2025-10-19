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
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public final class PlayerChatCompletionPacket implements MinecraftPacket {

  private final String[] completions;
  private final Action action;

  public PlayerChatCompletionPacket() {
    this(new String[0], Action.ADD);
  }

  public PlayerChatCompletionPacket(String[] completions, Action action) {
    this.completions = completions;
    this.action = action;
  }

  public String[] completions() {
    return completions;
  }

  public Action action() {
    return action;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public enum Action {
    ADD,
    REMOVE,
    SET
  }

  public static class Codec implements PacketCodec<PlayerChatCompletionPacket> {
    @Override
    public PlayerChatCompletionPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                              ProtocolVersion protocolVersion) {
      Action action = Action.values()[ProtocolUtils.readVarInt(buf)];
      String[] completions = ProtocolUtils.readStringArray(buf);
      return new PlayerChatCompletionPacket(completions, action);
    }

    @Override
    public void encode(PlayerChatCompletionPacket packet, ByteBuf buf,
                       ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      ProtocolUtils.writeVarInt(buf, packet.action.ordinal());
      ProtocolUtils.writeStringArray(buf, packet.completions);
    }
  }
}
