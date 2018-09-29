package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class TitlePacket implements MinecraftPacket {
    public static final int SET_TITLE = 0;
    public static final int SET_SUBTITLE = 1;
    public static final int SET_ACTION_BAR = 2;
    public static final int SET_TIMES = 3;
    public static final int SET_TIMES_OLD = 2;
    public static final int HIDE = 4;
    public static final int HIDE_OLD = 3;
    public static final int RESET = 5;
    public static final int RESET_OLD = 4;

    private int action;
    private String component;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        throw new UnsupportedOperationException(); // encode only
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeVarInt(buf, action);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_11) {
            // 1.11+ shifted the action enum by 1 to handle the action bar
            switch (action) {
                case SET_TITLE:
                case SET_SUBTITLE:
                case SET_ACTION_BAR:
                    ProtocolUtils.writeString(buf, component);
                    break;
                case SET_TIMES:
                    buf.writeInt(fadeIn);
                    buf.writeInt(stay);
                    buf.writeInt(fadeOut);
                    break;
                case HIDE:
                case RESET:
                    break;
            }
        } else {
            switch (action) {
                case SET_TITLE:
                case SET_SUBTITLE:
                    ProtocolUtils.writeString(buf, component);
                    break;
                case SET_TIMES_OLD:
                    buf.writeInt(fadeIn);
                    buf.writeInt(stay);
                    buf.writeInt(fadeOut);
                    break;
                case HIDE_OLD:
                case RESET_OLD:
                    break;
            }
        }
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public int getFadeIn() {
        return fadeIn;
    }

    public void setFadeIn(int fadeIn) {
        this.fadeIn = fadeIn;
    }

    public int getStay() {
        return stay;
    }

    public void setStay(int stay) {
        this.stay = stay;
    }

    public int getFadeOut() {
        return fadeOut;
    }

    public void setFadeOut(int fadeOut) {
        this.fadeOut = fadeOut;
    }

    public static TitlePacket hideForProtocolVersion(int protocolVersion) {
        TitlePacket packet = new TitlePacket();
        packet.setAction(protocolVersion >= ProtocolConstants.MINECRAFT_1_11 ? TitlePacket.HIDE : TitlePacket.HIDE_OLD);
        return packet;
    }

    public static TitlePacket resetForProtocolVersion(int protocolVersion) {
        TitlePacket packet = new TitlePacket();
        packet.setAction(protocolVersion >= ProtocolConstants.MINECRAFT_1_11 ? TitlePacket.RESET : TitlePacket.RESET_OLD);
        return packet;
    }

    public static TitlePacket timesForProtocolVersion(int protocolVersion) {
        TitlePacket packet = new TitlePacket();
        packet.setAction(protocolVersion >= ProtocolConstants.MINECRAFT_1_11 ? TitlePacket.SET_TIMES : TitlePacket.SET_TIMES_OLD);
        return packet;
    }

    @Override
    public String toString() {
        return "TitlePacket{" +
                "action=" + action +
                ", component='" + component + '\'' +
                ", fadeIn=" + fadeIn +
                ", stay=" + stay +
                ", fadeOut=" + fadeOut +
                '}';
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
