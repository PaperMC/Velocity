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

package com.velocitypowered.proxy.network.java.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundClientSettingsPacket implements Packet {
  public static final PacketReader<ServerboundClientSettingsPacket> DECODER = PacketReader.method(ServerboundClientSettingsPacket::new);
  public static final PacketWriter<ServerboundClientSettingsPacket> ENCODER = PacketWriter.deprecatedEncode();

  private @Nullable String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7 Protocol
  private short skinParts;
  private int mainHand;

  public ServerboundClientSettingsPacket() {
  }

  public ServerboundClientSettingsPacket(String locale, byte viewDistance, int chatVisibility, boolean chatColors,
                                         short skinParts, int mainHand) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolVersion version) {
    this.locale = ProtocolUtils.readString(buf, 16);
    this.viewDistance = buf.readByte();
    this.chatVisibility = ProtocolUtils.readVarInt(buf);
    this.chatColors = buf.readBoolean();

    if (version.lte(ProtocolVersion.MINECRAFT_1_7_6)) {
      this.difficulty = buf.readByte();
    }

    this.skinParts = buf.readUnsignedByte();

    if (version.gte(ProtocolVersion.MINECRAFT_1_9)) {
      this.mainHand = ProtocolUtils.readVarInt(buf);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (locale == null) {
      throw new IllegalStateException("No locale specified");
    }
    ProtocolUtils.writeString(buf, locale);
    buf.writeByte(viewDistance);
    ProtocolUtils.writeVarInt(buf, chatVisibility);
    buf.writeBoolean(chatColors);

    if (version.lte(ProtocolVersion.MINECRAFT_1_7_6)) {
      buf.writeByte(difficulty);
    }

    buf.writeByte(skinParts);

    if (version.gte(ProtocolVersion.MINECRAFT_1_9)) {
      ProtocolUtils.writeVarInt(buf, mainHand);
    }
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  public String getLocale() {
    if (locale == null) {
      throw new IllegalStateException("No locale specified");
    }
    return locale;
  }

  public byte getViewDistance() {
    return viewDistance;
  }

  public int getChatVisibility() {
    return chatVisibility;
  }

  public boolean isChatColors() {
    return chatColors;
  }

  public short getSkinParts() {
    return skinParts;
  }

  public int getMainHand() {
    return mainHand;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("locale", this.locale)
      .add("viewDistance", this.viewDistance)
      .add("chatVisibility", this.chatVisibility)
      .add("chatColors", this.chatColors)
      .add("difficulty", this.difficulty)
      .add("skinParts", this.skinParts)
      .add("mainHand", this.mainHand)
      .toString();
  }
}
