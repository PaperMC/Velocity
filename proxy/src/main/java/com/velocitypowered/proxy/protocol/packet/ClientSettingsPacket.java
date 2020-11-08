package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientSettingsPacket implements Packet {

  public static final Decoder<ClientSettingsPacket> DECODER = Decoder.method(ClientSettingsPacket::new);

  private @Nullable String locale;
  private byte viewDistance;
  private int chatVisibility;
  private boolean chatColors;
  private byte difficulty; // 1.7 Protocol
  private short skinParts;
  private int mainHand;

  public ClientSettingsPacket() {
  }

  public ClientSettingsPacket(String locale, byte viewDistance, int chatVisibility, boolean chatColors,
                              short skinParts, int mainHand) {
    this.locale = locale;
    this.viewDistance = viewDistance;
    this.chatVisibility = chatVisibility;
    this.chatColors = chatColors;
    this.skinParts = skinParts;
    this.mainHand = mainHand;
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

  @Override
  public String toString() {
    return "ClientSettingsPacket{"
        + "locale='" + locale + '\''
        + ", viewDistance=" + viewDistance
        + ", chatVisibility=" + chatVisibility
        + ", chatColors=" + chatColors
        + ", skinParts=" + skinParts
        + ", mainHand=" + mainHand
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
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
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
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
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
