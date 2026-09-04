/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;

public final class ClientboundPostEffectsPacket implements MinecraftPacket {

  private List<Key> postEffects;

  public ClientboundPostEffectsPacket() {
  }

  public ClientboundPostEffectsPacket(List<Key> postEffects) {
    this.postEffects = postEffects;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    int size = ProtocolUtils.readVarInt(buf);
    this.postEffects = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      this.postEffects.add(ProtocolUtils.readKey(buf));
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.postEffects.size());
    for (Key key : this.postEffects) {
      ProtocolUtils.writeKey(buf, key);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
