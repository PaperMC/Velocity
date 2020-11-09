package com.velocitypowered.proxy.connection.forge.legacy;

import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_RESET_DATA;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.MOD_LIST_DISCRIMINATOR;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPluginMessagePacket;
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
  static byte getHandshakePacketDiscriminator(AbstractPluginMessagePacket<?> message) {
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
  static List<ModInfo.Mod> readModList(AbstractPluginMessagePacket<?> message) {
    Preconditions.checkNotNull(message, "message");
    Preconditions
        .checkArgument(message.getChannel().equals(FORGE_LEGACY_HANDSHAKE_CHANNEL),
            "message is not a FML HS plugin message");

    ByteBuf byteBuf = message.content().retainedSlice();
    try {
      byte discriminator = byteBuf.readByte();

      if (discriminator == MOD_LIST_DISCRIMINATOR) {
        ImmutableList.Builder<ModInfo.Mod> mods = ImmutableList.builder();
        int modCount = ProtocolUtils.readVarInt(byteBuf);

        for (int index = 0; index < modCount; index++) {
          String id = ProtocolUtils.readString(byteBuf);
          String version = ProtocolUtils.readString(byteBuf);
          mods.add(new ModInfo.Mod(id, version));
        }

        return mods.build();
      }

      return ImmutableList.of();
    } finally {
      byteBuf.release();
    }
  }

  /**
   * Creates a reset packet.
   *
   * @return A copy of the reset packet
   */
  static AbstractPluginMessagePacket<?> resetPacket() {
    return new ClientboundPluginMessagePacket(
        FORGE_LEGACY_HANDSHAKE_CHANNEL,
        Unpooled.wrappedBuffer(FORGE_LEGACY_HANDSHAKE_RESET_DATA.clone())
    );
  }
}
