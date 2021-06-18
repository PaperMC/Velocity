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

package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler extends JavaPacketHandler {

  default boolean beforeHandle() {
    return false;
  }

  default void handleGeneric(Packet packet) {
  }

  default void handleUnknown(ByteBuf buf) {
  }

  default void connected() {
  }

  default void disconnected() {
  }

  default void activated() {
  }

  default void deactivated() {
  }

  default void exception(Throwable throwable) {
  }

  default void writabilityChanged() {
  }

  default void readCompleted() {
  }
}
