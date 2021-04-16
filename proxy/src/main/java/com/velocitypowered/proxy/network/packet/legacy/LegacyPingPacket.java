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

package com.velocitypowered.proxy.network.packet.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyPingPacket implements LegacyPacket, Packet {

  private final LegacyMinecraftPingVersion version;
  private final @Nullable InetSocketAddress vhost;

  public LegacyPingPacket(LegacyMinecraftPingVersion version) {
    this.version = version;
    this.vhost = null;
  }

  public LegacyPingPacket(LegacyMinecraftPingVersion version, InetSocketAddress vhost) {
    this.version = version;
    this.vhost = vhost;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public LegacyMinecraftPingVersion getVersion() {
    return version;
  }

  public @Nullable InetSocketAddress getVhost() {
    return vhost;
  }
}
