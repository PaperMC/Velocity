package io.minimum.minecraft.velocity.protocol.netty;

import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.compression.JavaVelocityCompressor;
import io.netty.channel.Channel;

public class MinecraftPipelineUtils {
    public static void strapPipelineForServer(Channel ch) {
        ch.pipeline().addLast("legacy-ping-decode", new LegacyPingDecoder());
        ch.pipeline().addLast("frame-decoder", new MinecraftVarintFrameDecoder());
        ch.pipeline().addLast("legacy-ping-encode", LegacyPingEncoder.INSTANCE);
        ch.pipeline().addLast("frame-encoder", MinecraftVarintLengthEncoder.INSTANCE);
        ch.pipeline().addLast("minecraft-decoder", new MinecraftDecoder(ProtocolConstants.Direction.TO_SERVER));
        ch.pipeline().addLast("minecraft-encoder", new MinecraftEncoder(ProtocolConstants.Direction.TO_CLIENT));
    }

    public static void strapPipelineForProxy(Channel ch) {
        ch.pipeline().addLast("legacy-ping-decode", new LegacyPingDecoder());
        ch.pipeline().addLast("frame-decoder", new MinecraftVarintFrameDecoder());
        ch.pipeline().addLast("legacy-ping-encode", LegacyPingEncoder.INSTANCE);
        ch.pipeline().addLast("frame-encoder", MinecraftVarintLengthEncoder.INSTANCE);
        ch.pipeline().addLast("minecraft-decoder", new MinecraftDecoder(ProtocolConstants.Direction.TO_CLIENT));
        ch.pipeline().addLast("minecraft-encoder", new MinecraftEncoder(ProtocolConstants.Direction.TO_SERVER));
    }

    public static void enableCompression(Channel ch, int threshold) {
        if (threshold == -1) {
            ch.pipeline().remove("compress-decoder");
            ch.pipeline().remove("compress-encoder");
            return;
        }

        JavaVelocityCompressor compressor = new JavaVelocityCompressor();
        MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(threshold, compressor);
        MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(threshold, compressor);

        ch.pipeline().addBefore("minecraft-decoder", "compress-decoder", decoder);
        ch.pipeline().addBefore("minecraft-encoder", "compress-encoder", encoder);
    }
}
