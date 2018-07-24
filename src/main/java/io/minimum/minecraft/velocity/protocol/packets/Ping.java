package io.minimum.minecraft.velocity.protocol.packets;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;

public class Ping implements MinecraftPacket {
    private long randomId;

    public long getRandomId() {
        return randomId;
    }

    public void setRandomId(long randomId) {
        this.randomId = randomId;
    }

    @Override
    public String toString() {
        return "Ping{" +
                "randomId=" + randomId +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        randomId = buf.readLong();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        buf.writeLong(randomId);
    }
}
