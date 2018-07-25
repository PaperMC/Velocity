package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Chat implements MinecraftPacket {
    private String message;
    private byte position;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte getPosition() {
        return position;
    }

    public void setPosition(byte position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "message='" + message + '\'' +
                ", position=" + position +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        message = ProtocolUtils.readString(buf);
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            position = buf.readByte();
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, message);
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            buf.writeByte(position);
        }
    }
}
