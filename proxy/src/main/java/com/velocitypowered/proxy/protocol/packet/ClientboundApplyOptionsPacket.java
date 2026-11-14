/*
 * Copyright (C) 2026 Velocity Contributors
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
import io.netty.buffer.ByteBuf;

/**
 * Asks the client to turn on connection options (26.4+). The client switches on inbound handling,
 * answers with {@link ServerboundOptionsAppliedPacket} in the old mode and then switches on
 * outbound handling. Vanilla only uses it for {@link ConnectionOptions#ENCRYPTION}.
 */
public class ClientboundApplyOptionsPacket implements MinecraftPacket {

  private byte options;

  public ClientboundApplyOptionsPacket() {
  }

  public ClientboundApplyOptionsPacket(byte options) {
    this.options = options;
  }

  public byte getOptions() {
    return options;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    this.options = buf.readByte();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    buf.writeByte(this.options);
  }

  @Override
  public int decodeExpectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
                                     ProtocolVersion version) {
    return 1;
  }

  @Override
  public int decodeExpectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
                                     ProtocolVersion version) {
    return 1;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "ClientboundApplyOptionsPacket{options=" + options + '}';
  }
}
