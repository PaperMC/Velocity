package io.minimum.minecraft.velocity.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class PacketWrapper {
    private final MinecraftPacket packet;
    private final ByteBuf buffer;

    public PacketWrapper(MinecraftPacket packet, ByteBuf buffer) {
        this.packet = packet;
        this.buffer = buffer;
    }

    public MinecraftPacket getPacket() {
        return packet;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return "PacketWrapper{" +
                "packet=" + packet +
                ", buffer=" + ByteBufUtil.hexDump(buffer) +
                '}';
    }
}
