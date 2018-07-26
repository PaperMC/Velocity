package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolConstants;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

public interface MinecraftPipelineUtils {
    String FRAME_DECODER = "frame-decoder";
    String FRAME_ENCODER = "frame-encoder";
    String LEGACY_PING_DECODER = "legacy-ping-decoder";
    String LEGACY_PING_ENCODER = "legacy-ping-encoder";
    String MINECRAFT_DECODER = "minecraft-decoder";
    String MINECRAFT_ENCODER = "minecraft-encoder";
    String READ_TIMEOUT = "read-timeout";

    static void strapPipelineForProxy(Channel ch) {
        ch.pipeline()
          .addLast(READ_TIMEOUT, new ReadTimeoutHandler(30, TimeUnit.SECONDS))
          .addLast(LEGACY_PING_DECODER, new LegacyPingDecoder())
          .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
          .addLast(LEGACY_PING_ENCODER, LegacyPingEncoder.INSTANCE)
          .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
          .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.TO_SERVER))
          .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.TO_CLIENT));
    }

    static void strapPipelineForBackend(Channel ch) {
        ch.pipeline()
          .addLast(READ_TIMEOUT, new ReadTimeoutHandler(30, TimeUnit.SECONDS))
          .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
          .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
          .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.TO_CLIENT))
          .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.TO_SERVER));
    }
}
