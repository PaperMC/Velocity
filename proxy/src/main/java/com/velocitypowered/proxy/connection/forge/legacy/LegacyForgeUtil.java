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

package com.velocitypowered.proxy.connection.forge.legacy;

import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_RESET_DATA;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.MOD_LIST_DISCRIMINATOR;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;

class LegacyForgeUtil {

  private LegacyForgeUtil() {
    throw new AssertionError();
  }

  /**
   * Gets the discriminator from the FML|HS packet (the first byte in the data).
   *
   * @param message The message to analyse
   * @return The discriminator
   */
  static byte getHandshakePacketDiscriminator(PluginMessagePacket message) {
    Preconditions.checkArgument(message.getChannel().equals(FORGE_LEGACY_HANDSHAKE_CHANNEL));
    Preconditions.checkArgument(message.content().isReadable());
    return message.content().getByte(0);
  }

  /**
   * Gets the mod list from the mod list packet and parses it.
   *
   * @param message The message
   * @return The list of mods. May be empty.
   */
  static List<ModInfo.Mod> readModList(PluginMessagePacket message) {
    Preconditions.checkNotNull(message, "message");
    Preconditions.checkArgument(message.getChannel().equals(FORGE_LEGACY_HANDSHAKE_CHANNEL),
        "message is not a FML HS plugin message");

    ByteBuf contents = message.content().slice();
    byte discriminator = contents.readByte();
    if (discriminator == MOD_LIST_DISCRIMINATOR) {
      ImmutableList.Builder<ModInfo.Mod> mods = ImmutableList.builder();
      int modCount = ProtocolUtils.readVarInt(contents);

      for (int index = 0; index < modCount; index++) {
        String id = ProtocolUtils.readString(contents);
        String version = ProtocolUtils.readString(contents);
        mods.add(new ModInfo.Mod(id, version));
      }

      return mods.build();
    }

    return ImmutableList.of();
  }

  /**
   * Creates a reset packet.
   *
   * @return A copy of the reset packet
   */
  static PluginMessagePacket resetPacket() {
    PluginMessagePacket msg = new PluginMessagePacket();
    msg.setChannel(FORGE_LEGACY_HANDSHAKE_CHANNEL);
    msg.replace(Unpooled.wrappedBuffer(FORGE_LEGACY_HANDSHAKE_RESET_DATA.clone()));
    return msg;
  }
}
