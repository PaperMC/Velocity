package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class ClientSettings implements MinecraftPacket {
    private String locale;
    private byte viewDistance;
    private int chatVisibility;
    private boolean chatColors;
    private short skinParts;
    private int mainHand;

    public String getLocale() {
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
        return "ClientSettings{" +
                "locale='" + locale + '\'' +
                ", viewDistance=" + viewDistance +
                ", chatVisibility=" + chatVisibility +
                ", chatColors=" + chatColors +
                ", skinParts=" + skinParts +
                ", mainHand=" + mainHand +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.locale = ProtocolUtils.readString(buf, 16);
        this.viewDistance = buf.readByte();
        this.chatVisibility = ProtocolUtils.readVarInt(buf);
        this.chatColors = buf.readBoolean();
        this.skinParts = buf.readUnsignedByte();
        this.mainHand = ProtocolUtils.readVarInt(buf);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, locale);
        buf.writeByte(viewDistance);
        ProtocolUtils.writeVarInt(buf, chatVisibility);
        buf.writeBoolean(chatColors);
        buf.writeByte(skinParts);
        ProtocolUtils.writeVarInt(buf, mainHand);
    }
}
