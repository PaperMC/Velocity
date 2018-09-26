package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler {

    PacketStatus handle(MinecraftPacket packet);

    default PacketStatus handleUnknown(ByteBuf buf) {
        // No-op: we'll release the buffer later.
        return PacketStatus.ALLOW;
    }

    default void connected() {

    }

    default void disconnected() {

    }

    default void activated() {

    }

    default void deactivated() {

    }

    default void exception(Throwable throwable) {

    }

    default void writabilityChanged() {

    }

    default PacketStatus writeToChannel(Object packet) {
        return PacketStatus.ALLOW;
    }

    default PacketStatus closeWith(Object msg) {
        return PacketStatus.ALLOW;
    }

    default int getPriority() {
        return 0;
    }

    /**
     * Only used in SessionHandler that was created by plugins
     */
    public static enum PacketStatus {
        ALLOW,
        CANCEL
    }
}
