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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.Lists;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Represents a packet sent to remove player information from the player list.
 * The packet contains a collection of {@link UUID}s representing the profiles to be removed.
 */
public class RemovePlayerInfoPacket implements MinecraftPacket {

  private Collection<UUID> profilesToRemove;

  public RemovePlayerInfoPacket() {
    this.profilesToRemove = new ArrayList<>();
  }

  public RemovePlayerInfoPacket(Collection<UUID> profilesToRemove) {
    this.profilesToRemove = profilesToRemove;
  }

  public Collection<UUID> getProfilesToRemove() {
    return profilesToRemove;
  }

  public void setProfilesToRemove(Collection<UUID> profilesToRemove) {
    this.profilesToRemove = profilesToRemove;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    int length = ProtocolUtils.readVarInt(buf);
    Collection<UUID> profilesToRemove = Lists.newArrayListWithCapacity(length);
    for (int idx = 0; idx < length; idx++) {
      profilesToRemove.add(ProtocolUtils.readUuid(buf));
    }
    this.profilesToRemove = profilesToRemove;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.profilesToRemove.size());
    for (UUID uuid : this.profilesToRemove) {
      ProtocolUtils.writeUuid(buf, uuid);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
