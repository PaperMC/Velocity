package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;

public class Chat implements MinecraftPacket {
    public static final byte CHAT = (byte) 0;
    private String message;
    private byte type;

    public Chat() {
    }

    public Chat(String message, byte type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "message='" + message + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        message = ProtocolUtils.readString(buf);
        if (direction == ProtocolConstants.Direction.CLIENTBOUND) {
            type = buf.readByte();
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, message);
        if (direction == ProtocolConstants.Direction.CLIENTBOUND) {
            buf.writeByte(type);
        }
    }

    public static Chat create(Component component) {
        return create(component, CHAT);
    }

    public static Chat create(Component component, byte type) {
        Preconditions.checkNotNull(component, "component");
        return new Chat(ComponentSerializers.JSON.serialize(component), type);
    }
}
