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

package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import net.kyori.adventure.text.Component;

public class ClientboundDisconnectPacket implements Packet {
  public static final PacketReader<ClientboundDisconnectPacket> DECODER = (buf, version) ->
      new ClientboundDisconnectPacket(ProtocolUtils.readString(buf));
  public static final PacketWriter<ClientboundDisconnectPacket> ENCODER = (out, packet, version) -> {
    ProtocolUtils.writeString(out, packet.reason);
  };

  private final String reason;

  public ClientboundDisconnectPacket(String reason) {
    this.reason = Preconditions.checkNotNull(reason, "reason");
  }

  public String getReason() {
    if (reason == null) {
      throw new IllegalStateException("No reason specified");
    }
    return reason;
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public static ClientboundDisconnectPacket create(Component component, ProtocolVersion version) {
    Preconditions.checkNotNull(component, "component");
    return new ClientboundDisconnectPacket(ProtocolUtils.getJsonChatSerializer(version).serialize(component));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("reason", this.reason)
      .toString();
  }
}
