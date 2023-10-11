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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientSettings implements MinecraftPacket {
  private @Nullable String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7 Protocol
  private short skinParts;
  private int mainHand;
  private boolean chatFilteringEnabled; // Added in 1.17
  private boolean clientListingAllowed; // Added in 1.18, overwrites server-list "anonymous" mode

  public ClientSettings() {
  }

  public ClientSettings(String locale, byte viewDistance, int chatVisibility, boolean chatColors,
      short skinParts, int mainHand, boolean chatFilteringEnabled, boolean clientListingAllowed) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
    this.clientListingAllowed = clientListingAllowed;
  }

  public String getLocale() {
    if (locale == null) {
      throw new IllegalStateException("No locale specified");
    }
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public byte getViewDistance() {
    return viewDistance;
  }

  public void setViewDistance(byte viewDistance) {
    this.viewDistance = viewDistance;
  }

  public int getChatVisibility() {
    return chatVisibility;
  }

  public void setChatVisibility(int chatVisibility) {
    this.chatVisibility = chatVisibility;
  }

  public boolean isChatColors() {
    return chatColors;
  }

  public void setChatColors(boolean chatColors) {
    this.chatColors = chatColors;
  }

  public short getSkinParts() {
    return skinParts;
  }

  public void setSkinParts(short skinParts) {
    this.skinParts = skinParts;
  }

  public int getMainHand() {
    return mainHand;
  }

  public void setMainHand(int mainHand) {
    this.mainHand = mainHand;
  }

  public boolean isChatFilteringEnabled() {
    return chatFilteringEnabled;
  }

  public void setChatFilteringEnabled(boolean chatFilteringEnabled) {
    this.chatFilteringEnabled = chatFilteringEnabled;
  }

  public boolean isClientListingAllowed() {
    return clientListingAllowed;
  }

  public void setClientListingAllowed(boolean clientListingAllowed) {
    this.clientListingAllowed = clientListingAllowed;
  }

  @Override
  public String toString() {
    return "ClientSettings{" + "locale='" + locale + '\'' + ", viewDistance=" + viewDistance +
        ", chatVisibility=" + chatVisibility + ", chatColors=" + chatColors + ", skinParts=" +
        skinParts + ", mainHand=" + mainHand + ", chatFilteringEnabled=" + chatFilteringEnabled +
        ", clientListingAllowed=" + clientListingAllowed + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.locale = ProtocolUtils.readString(buf, 16);
    this.viewDistance = buf.readByte();
    this.chatVisibility = ProtocolUtils.readVarInt(buf);
    this.chatColors = buf.readBoolean();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_7_6) <= 0) {
      this.difficulty = buf.readByte();
    }

    this.skinParts = buf.readUnsignedByte();

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      this.mainHand = ProtocolUtils.readVarInt(buf);

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
        this.chatFilteringEnabled = buf.readBoolean();

        if (version.compareTo(ProtocolVersion.MINECRAFT_1_18) >= 0) {
          this.clientListingAllowed = buf.readBoolean();
        }
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (locale == null) {
      throw new IllegalStateException("No locale specified");
    }
    ProtocolUtils.writeString(buf, locale);
    buf.writeByte(viewDistance);
    ProtocolUtils.writeVarInt(buf, chatVisibility);
    buf.writeBoolean(chatColors);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_7_6) <= 0) {
      buf.writeByte(difficulty);
    }

    buf.writeByte(skinParts);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      ProtocolUtils.writeVarInt(buf, mainHand);

      if (version.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
        buf.writeBoolean(chatFilteringEnabled);

        if (version.compareTo(ProtocolVersion.MINECRAFT_1_18) >= 0) {
          buf.writeBoolean(clientListingAllowed);
        }
      }
    }
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
    final ClientSettings that = (ClientSettings) o;
    return viewDistance == that.viewDistance
        && chatVisibility == that.chatVisibility
        && chatColors == that.chatColors
        && difficulty == that.difficulty
        && skinParts == that.skinParts
        && mainHand == that.mainHand
        && chatFilteringEnabled == that.chatFilteringEnabled
        && clientListingAllowed == that.clientListingAllowed
        && Objects.equals(locale, that.locale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        locale,
        viewDistance,
        chatVisibility,
        chatColors,
        difficulty,
        skinParts,
        mainHand,
        chatFilteringEnabled,
        clientListingAllowed);
  }
}
