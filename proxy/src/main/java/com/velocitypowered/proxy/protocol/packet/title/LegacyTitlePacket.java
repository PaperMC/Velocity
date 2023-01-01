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

package com.velocitypowered.proxy.protocol.packet.title;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyTitlePacket extends GenericTitlePacket {

  private @Nullable String component;
  private int fadeIn;
  private int stay;
  private int fadeOut;

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_11) < 0
        && getAction() == ActionType.SET_ACTION_BAR) {
      throw new IllegalStateException("Action bars are only supported on 1.11 and newer");
    }
    ProtocolUtils.writeVarInt(buf, getAction().getAction(version));

    switch (getAction()) {
      case SET_TITLE:
      case SET_SUBTITLE:
      case SET_ACTION_BAR:
        if (component == null) {
          throw new IllegalStateException("No component found for " + getAction());
        }
        ProtocolUtils.writeString(buf, component);
        break;
      case SET_TIMES:
        buf.writeInt(fadeIn);
        buf.writeInt(stay);
        buf.writeInt(fadeOut);
        break;
      case HIDE:
      case RESET:
        break;
      default:
        throw new UnsupportedOperationException("Unknown action " + getAction());
    }

  }

  @Override
  public void setAction(ActionType action) {
    super.setAction(action);
  }

  @Override
  public @Nullable String getComponent() {
    return component;
  }

  @Override
  public void setComponent(@Nullable String component) {
    this.component = component;
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
    return "GenericTitlePacket{"
        + "action=" + getAction()
        + ", component='" + component + '\''
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
