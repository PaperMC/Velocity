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

/**
 * Represents the client settings packet in Minecraft, which is sent by the client
 * to the server to communicate its settings such as locale, view distance, chat preferences,
 * skin customization, and other client-side configurations.
 */
public class ClientSettingsPacket implements MinecraftPacket {
  private @Nullable String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7 Protocol
  private short skinParts;
  private int mainHand;
  private boolean textFilteringEnabled; // Added in 1.17
  private boolean clientListingAllowed; // Added in 1.18, overwrites server-list "anonymous" mode
  private int particleStatus; // Added in 1.21.2

  public ClientSettingsPacket() {
  }

  /**
   * Constructs a new {@code ClientSettingsPacket} with the specified settings.
   *
   * @param locale the client's locale setting
   * @param viewDistance the view distance
   * @param chatVisibility the client's chat visibility setting
   * @param chatColors whether chat colors are enabled
   * @param skinParts the customization for skin parts
   * @param mainHand the client's main hand preference
   * @param textFilteringEnabled whether text filtering is enabled
   * @param clientListingAllowed whether the client allows listing
   * @param particleStatus whether particles are enabled
   */
  public ClientSettingsPacket(String locale, byte viewDistance, int chatVisibility, boolean chatColors,
                              short skinParts, int mainHand, boolean textFilteringEnabled, boolean clientListingAllowed,
                              int particleStatus) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
    this.textFilteringEnabled = textFilteringEnabled;
    this.clientListingAllowed = clientListingAllowed;
    this.particleStatus = particleStatus;
  }

  /**
   * Gets the client's locale.
   *
   * @return the locale
   * @throws IllegalStateException if no locale is specified
   */
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

  public boolean isTextFilteringEnabled() {
    return textFilteringEnabled;
  }

  public void setTextFilteringEnabled(boolean textFilteringEnabled) {
    this.textFilteringEnabled = textFilteringEnabled;
  }

  public boolean isClientListingAllowed() {
    return clientListingAllowed;
  }

  public void setClientListingAllowed(boolean clientListingAllowed) {
    this.clientListingAllowed = clientListingAllowed;
  }

  public int getParticleStatus() {
    return particleStatus;
  }

  public void setParticleStatus(int particleStatus) {
    this.particleStatus = particleStatus;
  }

  @Override
  public String toString() {
    return "ClientSettings{" + "locale='" + locale + '\'' + ", viewDistance=" + viewDistance
        + ", chatVisibility=" + chatVisibility + ", chatColors=" + chatColors + ", skinParts="
        + skinParts + ", mainHand=" + mainHand + ", chatFilteringEnabled=" + textFilteringEnabled
        + ", clientListingAllowed=" + clientListingAllowed +  ", particleStatus=" + particleStatus + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.locale = ProtocolUtils.readString(buf, 16);
    this.viewDistance = buf.readByte();
    this.chatVisibility = ProtocolUtils.readVarInt(buf);
    this.chatColors = buf.readBoolean();

    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      this.difficulty = buf.readByte();
    }

    this.skinParts = buf.readUnsignedByte();

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      this.mainHand = ProtocolUtils.readVarInt(buf);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        this.textFilteringEnabled = buf.readBoolean();

        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          this.clientListingAllowed = buf.readBoolean();

          if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
            this.particleStatus = ProtocolUtils.readVarInt(buf);
          }
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

    if (version.noGreaterThan(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeByte(difficulty);
    }

    buf.writeByte(skinParts);

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      ProtocolUtils.writeVarInt(buf, mainHand);

      if (version.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        buf.writeBoolean(textFilteringEnabled);

        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_18)) {
          buf.writeBoolean(clientListingAllowed);
        }

        if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_2)) {
          ProtocolUtils.writeVarInt(buf, particleStatus);
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
            textFilteringEnabled,
        clientListingAllowed,
        particleStatus);
  }
}
