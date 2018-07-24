package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler {
    void handle(MinecraftPacket packet);

    default void handleUnknown(ByteBuf buf) {
        // No-op: we'll release the buffer later.
    }

    default void connectionClosed() {

    }
}
