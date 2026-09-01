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

package com.velocitypowered.proxy.protocol.packet.title;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class TitleClearPacket extends GenericTitlePacket {

  private final ActionType action;

  public TitleClearPacket() {
    this(ActionType.HIDE);
  }

  public TitleClearPacket(ActionType action) {
    if (action != ActionType.HIDE && action != ActionType.RESET) {
      throw new IllegalArgumentException("TitleClearPacket only accepts the HIDE and RESET actions.");
    }
    this.action = action;
  }

  @Override
  public @NotNull ActionType getAction() {
    return action;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeBoolean(this.action == ActionType.RESET);
  }

  @Override
  public String toString() {
    return "TitleClearPacket{"
        + ", resetTimes=" + (this.action == ActionType.RESET)
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
