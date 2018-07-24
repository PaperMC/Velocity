package io.minimum.minecraft.velocity.protocol;

import io.netty.buffer.ByteBuf;

public interface MinecraftPacket {
    void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion);

    void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion);
}
