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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyPing implements MinecraftPacket {

  private final LegacyMinecraftPingVersion version;
  private final @Nullable InetSocketAddress vhost;

  public LegacyPing(LegacyMinecraftPingVersion version) {
    this.version = version;
    this.vhost = null;
  }

  public LegacyPing(LegacyMinecraftPingVersion version, InetSocketAddress vhost) {
    this.version = version;
    this.vhost = vhost;
  }

  public LegacyMinecraftPingVersion getVersion() {
    return version;
  }

  public @Nullable InetSocketAddress getVhost() {
    return vhost;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
