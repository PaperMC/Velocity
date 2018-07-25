package io.minimum.minecraft.velocity.protocol.packets;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class SetCompression implements MinecraftPacket {
    private int threshold;

    public SetCompression() {}

    public SetCompression(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return "SetCompression{" +
                "threshold=" + threshold +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.threshold = ProtocolUtils.readVarInt(buf);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeVarInt(buf, threshold);
    }
}
