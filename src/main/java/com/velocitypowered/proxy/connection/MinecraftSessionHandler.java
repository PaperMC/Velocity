package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler {
    void handle(MinecraftPacket packet) throws Exception;

    default void handleUnknown(ByteBuf buf) {
        // No-op: we'll release the buffer later.
    }

    default void connected() {

    }

    default void disconnected() {

    }

    default void activated() {

    }

    default void exception(Throwable throwable) {

    }
}
