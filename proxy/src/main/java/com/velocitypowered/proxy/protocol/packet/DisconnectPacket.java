/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;

public record DisconnectPacket(StateRegistry state, ComponentHolder reason)
    implements MinecraftPacket {

  public DisconnectPacket {
    Preconditions.checkNotNull(reason, "reason");
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static DisconnectPacket create(Component component, ProtocolVersion version,
      StateRegistry state) {
    Preconditions.checkNotNull(component, "component");
    return new DisconnectPacket(state, new ComponentHolder(state == StateRegistry.LOGIN
            ? ProtocolVersion.MINECRAFT_1_20_2 : version, component));
  }

  public static class Codec implements PacketCodec<DisconnectPacket> {
    private final StateRegistry state;

    public Codec(StateRegistry state) {
      this.state = state;
    }

    @Override
    public DisconnectPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      ComponentHolder reason = ComponentHolder.read(buf, state == StateRegistry.LOGIN
              ? ProtocolVersion.MINECRAFT_1_20_2 : protocolVersion);
      return new DisconnectPacket(state, reason);
    }

    @Override
    public void encode(DisconnectPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      packet.reason().write(buf);
    }
  }
}
