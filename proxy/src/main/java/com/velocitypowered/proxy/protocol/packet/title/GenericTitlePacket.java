/*
 * Copyright (C) 2021-2023 Velocity Contributors
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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;

public abstract class GenericTitlePacket implements MinecraftPacket {

  public enum ActionType {
    SET_TITLE(0),
    SET_SUBTITLE(1),
    SET_ACTION_BAR(2),
    SET_TIMES(3),
    HIDE(4),
    RESET(5);

    private final int action;

    ActionType(int action) {
      this.action = action;
    }

    public int getAction(ProtocolVersion version) {
      return version.lessThan(ProtocolVersion.MINECRAFT_1_11)
          ? action > 2 ? action - 1 : action : action;
    }
  }


  private ActionType action;

  protected void setAction(ActionType action) {
    this.action = action;
  }

  public final ActionType getAction() {
    return action;
  }

  public ComponentHolder getComponent() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public void setComponent(ComponentHolder component) {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getFadeIn() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public void setFadeIn(int fadeIn) {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getStay() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public void setStay(int stay) {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getFadeOut() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public void setFadeOut(int fadeOut) {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }


  @Override
  public final void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    throw new UnsupportedOperationException(); // encode only
  }

  /**
   * Creates a version and type dependent TitlePacket.
   *
   * @param type    Action the packet should invoke
   * @param version Protocol version of the target player
   * @return GenericTitlePacket instance that follows the invoker type/version
   */
  public static GenericTitlePacket constructTitlePacket(ActionType type, ProtocolVersion version) {
    GenericTitlePacket packet = null;
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      packet = switch (type) {
        case SET_ACTION_BAR -> new TitleActionbarPacket();
        case SET_SUBTITLE -> new TitleSubtitlePacket();
        case SET_TIMES -> new TitleTimesPacket();
        case SET_TITLE -> new TitleTextPacket();
        case HIDE, RESET -> new TitleClearPacket();
        default -> throw new IllegalArgumentException("Invalid ActionType");
      };
    } else {
      packet = new LegacyTitlePacket();
    }
    packet.setAction(type);
    return packet;
  }

}
