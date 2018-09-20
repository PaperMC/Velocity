package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class StatusRequest implements MinecraftPacket {
    public static final StatusRequest INSTANCE = new StatusRequest();

    private StatusRequest() {

    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {

    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {

    }

    @Override
    public String toString() {
        return "StatusRequest";
    }
}
