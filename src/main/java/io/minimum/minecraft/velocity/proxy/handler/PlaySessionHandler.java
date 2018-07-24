package io.minimum.minecraft.velocity.proxy.handler;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public class PlaySessionHandler implements MinecraftSessionHandler {
    @Override
    public void handle(MinecraftPacket packet) {

    }

    @Override
    public void handleUnknown(ByteBuf buf) {

    }
}
