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
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StatusResponsePacket implements MinecraftPacket {

  private @Nullable CharSequence status;

  public StatusResponsePacket() {
  }

  public StatusResponsePacket(CharSequence status) {
    this.status = status;
  }

  public String getStatus() {
    if (status == null) {
      throw new IllegalStateException("Status is not specified");
    }
    return status.toString();
  }

  @Override
  public String toString() {
    return "StatusResponse{"
        + "status='" + status + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    status = ProtocolUtils.readString(buf, Short.MAX_VALUE);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (status == null) {
      throw new IllegalStateException("Status is not specified");
    }
    ProtocolUtils.writeString(buf, status);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
