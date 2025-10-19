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
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;

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

  private final ActionType action;

  protected GenericTitlePacket(ActionType action) {
    this.action = action;
  }

  public final ActionType getAction() {
    return action;
  }

  public ComponentHolder getComponent() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getFadeIn() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getStay() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  public int getFadeOut() {
    throw new UnsupportedOperationException("Invalid function for this TitlePacket ActionType");
  }

  /**
   * Creates a version and type dependent TitlePacket for HIDE/RESET actions.
   *
   * @param type    Action the packet should invoke (HIDE or RESET)
   * @param version Protocol version of the target player
   * @return GenericTitlePacket instance that follows the invoker type/version
   */
  public static GenericTitlePacket createClearTitlePacket(ActionType type, ProtocolVersion version) {
    if (type != ActionType.HIDE && type != ActionType.RESET) {
      throw new IllegalArgumentException("createClearTitlePacket only accepts HIDE and RESET actions");
    }
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      return new TitleClearPacket(type == ActionType.RESET);
    } else {
      return new LegacyTitlePacket(type, null, 0, 0, 0);
    }
  }

  /**
   * Creates a version and type dependent TitlePacket for component-based actions.
   *
   * @param type      Action the packet should invoke
   * @param component Component to display
   * @param version   Protocol version of the target player
   * @return GenericTitlePacket instance that follows the invoker type/version
   */
  public static GenericTitlePacket createComponentTitlePacket(ActionType type,
      ComponentHolder component, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      switch (type) {
        case SET_ACTION_BAR:
          return new TitleActionbarPacket(component);
        case SET_SUBTITLE:
          return new TitleSubtitlePacket(component);
        case SET_TITLE:
          return new TitleTextPacket(component);
        default:
          throw new IllegalArgumentException("Invalid ActionType for component title: " + type);
      }
    } else {
      return new LegacyTitlePacket(type, component, 0, 0, 0);
    }
  }

  /**
   * Creates a version dependent TitlePacket for times.
   *
   * @param fadeIn  Fade in time
   * @param stay    Stay time
   * @param fadeOut Fade out time
   * @param version Protocol version of the target player
   * @return GenericTitlePacket instance that follows the invoker type/version
   */
  public static GenericTitlePacket createTimesTitlePacket(int fadeIn, int stay, int fadeOut,
      ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      return new TitleTimesPacket(fadeIn, stay, fadeOut);
    } else {
      return new LegacyTitlePacket(ActionType.SET_TIMES, null, fadeIn, stay, fadeOut);
    }
  }
}
