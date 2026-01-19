/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class ClientSettingsPacket implements MinecraftPacket {
  private final @Nullable String locale;
  private final byte viewDistance;
  private final int chatVisibility;
  private final boolean chatColors;
  private final byte difficulty; // 1.7 Protocol
  private final short skinParts;
  private final int mainHand;
  private final boolean textFilteringEnabled; // Added in 1.17
  private final boolean clientListingAllowed; // Added in 1.18, overwrites server-list "anonymous" mode
  private final int particleStatus; // Added in 1.21.2

  public ClientSettingsPacket() {
    this(null, (byte) 0, 0, false, (byte) 0, (short) 0, 0, false, false, 0);
  }

  public ClientSettingsPacket(String locale, byte viewDistance, int chatVisibility,
                               boolean chatColors, short skinParts, int mainHand,
                               boolean textFilteringEnabled, boolean clientListingAllowed,
                               int particleStatus) {
    this(locale, viewDistance, chatVisibility, chatColors, (byte) 0, skinParts, mainHand,
        textFilteringEnabled, clientListingAllowed, particleStatus);
  }

  public ClientSettingsPacket(String locale, byte viewDistance, int chatVisibility,
                               boolean chatColors, byte difficulty, short skinParts, int mainHand,
                               boolean textFilteringEnabled, boolean clientListingAllowed,
                               int particleStatus) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.difficulty = difficulty;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
    this.textFilteringEnabled = textFilteringEnabled;
    this.clientListingAllowed = clientListingAllowed;
    this.particleStatus = particleStatus;
  }

  public String locale() {
    if (locale == null) {
      throw new IllegalStateException("No locale specified");
    }
    return locale;
  }

  public byte viewDistance() {
    return viewDistance;
  }

  public int chatVisibility() {
    return chatVisibility;
  }

  public boolean chatColors() {
    return chatColors;
  }

  public byte difficulty() {
    return difficulty;
  }

  public short skinParts() {
    return skinParts;
  }

  public int mainHand() {
    return mainHand;
  }

  public boolean textFilteringEnabled() {
    return textFilteringEnabled;
  }

  public boolean clientListingAllowed() {
    return clientListingAllowed;
  }

  public int particleStatus() {
    return particleStatus;
  }

  public String getLocale() {
    return locale();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ClientSettingsPacket that = (ClientSettingsPacket) o;
    return viewDistance == that.viewDistance
        && chatVisibility == that.chatVisibility
        && chatColors == that.chatColors
        && difficulty == that.difficulty
        && skinParts == that.skinParts
        && mainHand == that.mainHand
        && textFilteringEnabled == that.textFilteringEnabled
        && clientListingAllowed == that.clientListingAllowed
        && particleStatus == that.particleStatus
        && Objects.equals(locale, that.locale);
  }

  public static class Codec implements PacketCodec<ClientSettingsPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ClientSettingsPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                        ProtocolVersion version) {
      String locale = ProtocolUtils.readString(buf, 16);
      byte viewDistance = buf.readByte();
      int chatVisibility = ProtocolUtils.readVarInt(buf);
      boolean chatColors = buf.readBoolean();
      byte difficulty = 0;

      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        difficulty = buf.readByte();
      }

      short skinParts = buf.readUnsignedByte();
      int mainHand = 0;
      boolean textFilteringEnabled = false;
      boolean clientListingAllowed = false;
      int particleStatus = 0;

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
        mainHand = ProtocolUtils.readVarInt(buf);

        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
          textFilteringEnabled = buf.readBoolean();

          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
            clientListingAllowed = buf.readBoolean();

            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
              particleStatus = ProtocolUtils.readVarInt(buf);
            }
          }
        }
      }

      return new ClientSettingsPacket(locale, viewDistance, chatVisibility, chatColors,
          difficulty, skinParts, mainHand, textFilteringEnabled, clientListingAllowed,
          particleStatus);
    }

    @Override
    public void encode(ClientSettingsPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      if (packet.locale == null) {
        throw new IllegalStateException("No locale specified");
      }
      ProtocolUtils.writeString(buf, packet.locale);
      buf.writeByte(packet.viewDistance);
      ProtocolUtils.writeVarInt(buf, packet.chatVisibility);
      buf.writeBoolean(packet.chatColors);

      if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
        buf.writeByte(packet.difficulty);
      }

      buf.writeByte(packet.skinParts);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
        ProtocolUtils.writeVarInt(buf, packet.mainHand);

        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
          buf.writeBoolean(packet.textFilteringEnabled);

          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
            buf.writeBoolean(packet.clientListingAllowed);
          }

          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
            ProtocolUtils.writeVarInt(buf, packet.particleStatus);
          }
        }
      }
    }
  }
}
