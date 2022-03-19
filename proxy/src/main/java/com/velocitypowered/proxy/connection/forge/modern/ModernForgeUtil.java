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

package com.velocitypowered.proxy.connection.forge.modern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

class ModernForgeUtil {

  private ModernForgeUtil() {
    throw new AssertionError();
  }

  static List<ModInfo.Mod> readModList(LoginPluginResponse message) {
    Preconditions.checkNotNull(message, "message");
    ByteBuf buf = message.content().slice();

    String channel = ProtocolUtils.readString(buf);
    if (!channel.equals(ModernForgeConstants.HANDSHAKE_CHANNEL)) {
      return null;
    }

    int payloadLength = ProtocolUtils.readVarInt(buf);
    if (payloadLength < 1) {
      return null;
    }

    int discriminator = buf.readUnsignedByte();
    if (discriminator != ModernForgeConstants.MOD_LIST_DISCRIMINATOR) {
      return null;
    }

    ImmutableList.Builder<ModInfo.Mod> mods = ImmutableList.builder();
    int modLength = ProtocolUtils.readVarInt(buf);
    for (int index = 0; index < modLength; index++) {
      String id = ProtocolUtils.readString(buf, 256);
      mods.add(new ModInfo.Mod(id));
    }

    return mods.build();
  }

  static MinecraftPacket resetPacket(StateRegistry state) {
    ByteBuf buf = Unpooled.buffer();
    // Channel
    ProtocolUtils.writeString(buf, ModernForgeConstants.HANDSHAKE_CHANNEL);
    // Payload Length
    ProtocolUtils.writeVarInt(buf, 1);
    // Discriminator
    buf.writeByte(ModernForgeConstants.RESET_DISCRIMINATOR & 0xFF);

    switch (state) {
      case LOGIN: {
        return new LoginPluginMessage(
            ModernForgeConstants.RESET_DISCRIMINATOR,
            ModernForgeConstants.LOGIN_WRAPPER_CHANNEL,
            buf);
      }
      case PLAY: {
        return new PluginMessage(
            ModernForgeConstants.LOGIN_WRAPPER_CHANNEL,
            buf);
      }
      default: {
        throw new UnsupportedOperationException("Unsupported state " + state);
      }
    }
  }
}
