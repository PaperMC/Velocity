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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The {@code LegacyTitlePacket} class represents a packet that handles title-related functionality
 * for older versions of Minecraft where title handling differs.
 *
 * <p>This packet is used to send title and subtitle information using legacy methods for clients
 * that do not support the newer title packet format.</p>
 *
 * <p>It extends the {@link GenericTitlePacket}, inheriting basic title properties but is specifically
 * focused on legacy title implementations.</p>
 */
public class LegacyTitlePacket extends GenericTitlePacket {

  private @Nullable ComponentHolder component;
  private int fadeIn;
  private int stay;
  private int fadeOut;

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.lessThan(ProtocolVersion.MINECRAFT_1_11)
        && getAction() == ActionType.SET_ACTION_BAR) {
      throw new IllegalStateException("Action bars are only supported on 1.11 and newer");
    }
    ProtocolUtils.writeVarInt(buf, getAction().getAction(version));

    switch (getAction()) {
      case SET_TITLE, SET_SUBTITLE, SET_ACTION_BAR -> {
        if (component == null) {
          throw new IllegalStateException("No component found for " + getAction());
        }
        component.write(buf);
      }
      case SET_TIMES -> {
        buf.writeInt(fadeIn);
        buf.writeInt(stay);
        buf.writeInt(fadeOut);
      }
      case HIDE, RESET -> {
      }
      default -> throw new UnsupportedOperationException("Unknown action " + getAction());
    }
  }

  @Override
  public void setAction(ActionType action) {
    super.setAction(action);
  }

  @Override
  public @Nullable ComponentHolder getComponent() {
    return component;
  }

  @Override
  public void setComponent(@Nullable ComponentHolder component) {
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
