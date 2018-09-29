package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class BossBar implements MinecraftPacket {
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int UPDATE_PERCENT = 2;
    public static final int UPDATE_NAME = 3;
    public static final int UPDATE_STYLE = 4;
    public static final int UPDATE_PROPERTIES = 5;
    private UUID uuid;
    private int action;
    private String name;
    private float percent;
    private int color;
    private int overlay;
    private short flags;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getOverlay() {
        return overlay;
    }

    public void setOverlay(int overlay) {
        this.overlay = overlay;
    }

    public short getFlags() {
        return flags;
    }

    public void setFlags(short flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "BossBar{" +
                "uuid=" + uuid +
                ", action=" + action +
                ", name='" + name + '\'' +
                ", percent=" + percent +
                ", color=" + color +
                ", overlay=" + overlay +
                ", flags=" + flags +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.uuid = ProtocolUtils.readUuid(buf);
        this.action = ProtocolUtils.readVarInt(buf);
        switch (action) {
            case ADD:
                this.name = ProtocolUtils.readString(buf);
                this.percent = buf.readFloat();
                this.color = ProtocolUtils.readVarInt(buf);
                this.overlay = ProtocolUtils.readVarInt(buf);
                this.flags = buf.readUnsignedByte();
                break;
            case REMOVE:
                break;
            case UPDATE_PERCENT:
                this.percent = buf.readFloat();
                break;
            case UPDATE_NAME:
                this.name = ProtocolUtils.readString(buf);
                break;
            case UPDATE_STYLE:
                this.color = ProtocolUtils.readVarInt(buf);
                this.overlay = ProtocolUtils.readVarInt(buf);
                break;
            case UPDATE_PROPERTIES:
                this.flags = buf.readUnsignedByte();
                break;
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeUuid(buf, uuid);
        ProtocolUtils.writeVarInt(buf, action);
        switch (action) {
            case ADD:
                ProtocolUtils.writeString(buf, name);
                buf.writeFloat(percent);
                ProtocolUtils.writeVarInt(buf, color);
                ProtocolUtils.writeVarInt(buf, overlay);
                buf.writeByte(flags);
                break;
            case REMOVE:
                break;
            case UPDATE_PERCENT:
                buf.writeFloat(percent);
                break;
            case UPDATE_NAME:
                ProtocolUtils.writeString(buf, name);
                break;
            case UPDATE_STYLE:
                ProtocolUtils.writeVarInt(buf, color);
                ProtocolUtils.writeVarInt(buf, overlay);
                break;
            case UPDATE_PROPERTIES:
                buf.writeByte(flags);
                break;
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
