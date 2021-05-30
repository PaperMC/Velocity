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

package com.velocitypowered.proxy.network.java.packet;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.function.LongFunction;

public abstract class AbstractStatusPingPacket implements Packet {
  protected static <P extends AbstractStatusPingPacket> PacketReader<P> decoder(final LongFunction<P> factory) {
    return new PacketReader<P>() {
      @Override
      public P read(ByteBuf buf, ProtocolVersion version) {
        final long randomId = buf.readLong();
        return factory.apply(randomId);
      }

      @Override
      public int expectedMaxLength(ByteBuf buf, ProtocolVersion version) {
        return 8;
      }

      @Override
      public int expectedMinLength(ByteBuf buf, ProtocolVersion version) {
        return 8;
      }
    };
  }

  protected static <P extends AbstractStatusPingPacket> PacketWriter<P> encoder() {
    return (buf, packet, version) -> buf.writeLong(packet.getRandomId());
  }

  private final long randomId;

  protected AbstractStatusPingPacket(final long randomId) {
    this.randomId = randomId;
  }

  public long getRandomId() {
    return randomId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("randomId", this.randomId)
      .toString();
  }
}
