package com.velocitypowered.proxy.protocol.packets;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;

public class Chat implements MinecraftPacket {
    private String message;
    private byte position;

    public Chat() {
    }

    public Chat(String message, byte position) {
        this.message = message;
        this.position = position;
    }

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

    public static Chat create(Component component) {
        return create(component, (byte) 0);
    }

    public static Chat create(Component component, byte pos) {
        Preconditions.checkNotNull(component, "component");
        return new Chat(ComponentSerializers.JSON.serialize(component), pos);
    }
}
