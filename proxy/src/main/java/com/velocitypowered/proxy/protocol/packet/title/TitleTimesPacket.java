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

public class TitleTimesPacket extends GenericTitlePacket {

  private int fadeIn;
  private int stay;
  private int fadeOut;

  public TitleTimesPacket() {
    setAction(ActionType.SET_TIMES);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    buf.writeInt(fadeIn);
    buf.writeInt(stay);
    buf.writeInt(fadeOut);
  }

  @Override
  public int getFadeIn() {
    return fadeIn;
  }

  @Override
  public void setFadeIn(int fadeIn) {
    this.fadeIn = fadeIn;
  }

  @Override
  public int getStay() {
    return stay;
  }

  @Override
  public void setStay(int stay) {
    this.stay = stay;
  }

  @Override
  public int getFadeOut() {
    return fadeOut;
  }

  @Override
  public void setFadeOut(int fadeOut) {
    this.fadeOut = fadeOut;
  }

  @Override
  public String toString() {
    return "TitleTimesPacket{"
        + ", fadeIn=" + fadeIn
        + ", stay=" + stay
        + ", fadeOut=" + fadeOut
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
