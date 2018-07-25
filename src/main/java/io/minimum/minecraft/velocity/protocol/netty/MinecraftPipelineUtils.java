package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

public class MinecraftPipelineUtils {
    public static void strapPipelineForServer(Channel ch) {
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(30, TimeUnit.SECONDS));
        ch.pipeline().addLast("legacy-ping-decode", new LegacyPingDecoder());
        ch.pipeline().addLast("frame-decoder", new MinecraftVarintFrameDecoder());
        ch.pipeline().addLast("legacy-ping-encode", LegacyPingEncoder.INSTANCE);
        ch.pipeline().addLast("frame-encoder", MinecraftVarintLengthEncoder.INSTANCE);
        ch.pipeline().addLast("minecraft-decoder", new MinecraftDecoder(ProtocolConstants.Direction.TO_SERVER));
        ch.pipeline().addLast("minecraft-encoder", new MinecraftEncoder(ProtocolConstants.Direction.TO_CLIENT));
    }

    public static void strapPipelineForProxy(Channel ch) {
        ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(30, TimeUnit.SECONDS));
        ch.pipeline().addLast("legacy-ping-decode", new LegacyPingDecoder());
        ch.pipeline().addLast("frame-decoder", new MinecraftVarintFrameDecoder());
        ch.pipeline().addLast("legacy-ping-encode", LegacyPingEncoder.INSTANCE);
        ch.pipeline().addLast("frame-encoder", MinecraftVarintLengthEncoder.INSTANCE);
        ch.pipeline().addLast("minecraft-decoder", new MinecraftDecoder(ProtocolConstants.Direction.TO_CLIENT));
        ch.pipeline().addLast("minecraft-encoder", new MinecraftEncoder(ProtocolConstants.Direction.TO_SERVER));
    }
}
