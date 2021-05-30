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

package com.velocitypowered.proxy.network.java.packet.serverbound;

import com.velocitypowered.proxy.network.java.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;

public class ServerboundPluginMessagePacket extends AbstractPluginMessagePacket<ServerboundPluginMessagePacket> implements Packet {
  public static final Factory<ServerboundPluginMessagePacket> FACTORY = ServerboundPluginMessagePacket::new;
  public static final PacketReader<ServerboundPluginMessagePacket> DECODER = decoder(FACTORY);
  public static final PacketWriter<ServerboundPluginMessagePacket> ENCODER = encoder();

  public ServerboundPluginMessagePacket(final String channel, final ByteBuf backing) {
    super(channel, backing);
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  @Override
  public ServerboundPluginMessagePacket replace(ByteBuf content) {
    return new ServerboundPluginMessagePacket(this.channel, content);
  }
}
