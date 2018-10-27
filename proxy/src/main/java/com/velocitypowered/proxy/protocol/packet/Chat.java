package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Chat implements MinecraftPacket {
    public static final byte CHAT = (byte) 0;
    public static final int MAX_SERVERBOUND_MESSAGE_LENGTH = 256;

    private @Nullable String message;
    private byte type;

    public Chat() {
    }

    public Chat(String message, byte type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        if (message == null) {
            throw new IllegalStateException("Message is not specified");
        }
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
        if (message == null) {
            throw new IllegalStateException("Message is not specified");
        }
        ProtocolUtils.writeString(buf, message);
        if (direction == ProtocolConstants.Direction.CLIENTBOUND) {
            buf.writeByte(type);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }

    public static Chat createClientbound(Component component) {
        return createClientbound(component, CHAT);
    }

    public static Chat createClientbound(Component component, byte type) {
        Preconditions.checkNotNull(component, "component");
        return new Chat(ComponentSerializers.JSON.serialize(component), type);
    }

    public static Chat createServerbound(String message) {
        return new Chat(message, CHAT);
    }
}
