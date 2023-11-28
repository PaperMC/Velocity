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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;

public class ActiveFeatures implements MinecraftPacket {

  private Key[] activeFeatures;

  public ActiveFeatures(Key[] activeFeatures) {
    this.activeFeatures = activeFeatures;
  }

  public ActiveFeatures() {
    this.activeFeatures = new Key[0];
  }

  public void setActiveFeatures(Key[] activeFeatures) {
    this.activeFeatures = activeFeatures;
  }

  public Key[] getActiveFeatures() {
    return activeFeatures;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    activeFeatures = ProtocolUtils.readKeyArray(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    ProtocolUtils.writeKeyArray(buf, activeFeatures);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
