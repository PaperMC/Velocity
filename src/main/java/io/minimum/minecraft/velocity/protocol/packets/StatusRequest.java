package io.minimum.minecraft.velocity.protocol.packets;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class StatusRequest implements MinecraftPacket {
    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {

    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {

    }
}
