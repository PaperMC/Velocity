/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.netty;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.proxy.network.limiter.PacketLimiter;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Decompresses a Minecraft packet.
 */
public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

  private static final int SERVERBOUND_MAXIMUM_UNCOMPRESSED_SIZE = 2 * 1024 * 1024; // 2MiB
  private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB
  private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 128 * 1024 * 1024; // 128MiB

  private static final int CLIENTBOUND_UNCOMPRESSED_CAP =
      Boolean.getBoolean("velocity.increased-compression-cap")
          ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;
  private static final int SERVERBOUND_UNCOMPRESSED_CAP =
          Boolean.getBoolean("velocity.increased-compression-cap")
                  ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : SERVERBOUND_MAXIMUM_UNCOMPRESSED_SIZE;
  private static final boolean SKIP_COMPRESSION_VALIDATION = Boolean.getBoolean("velocity.skip-uncompressed-packet-size-validation");
  private final ProtocolUtils.Direction direction;

  private int threshold;
  private final VelocityCompressor compressor;
  @Nullable
  private PacketLimiter packetLimiter;

  /**
   * Creates a new {@code MinecraftCompressDecoder} with the specified compression {@code threshold}.
   *
   * @param threshold the threshold for compression. Packets with uncompressed size below this threshold will not be compressed.
   * @param compressor the compressor instance to use
   * @param direction the direction of the packets being decoded
   */
  public MinecraftCompressDecoder(int threshold, VelocityCompressor compressor, ProtocolUtils.Direction direction) {
    this.threshold = threshold;
    this.compressor = compressor;
    this.direction = direction;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int claimedUncompressedSize = ProtocolUtils.readVarInt(in);
    if (claimedUncompressedSize == 0) {
      if (!SKIP_COMPRESSION_VALIDATION) {
        int actualUncompressedSize = in.readableBytes();
        checkFrame(actualUncompressedSize < threshold, "Actual uncompressed size %s is greater than"
            + " threshold %s", actualUncompressedSize, threshold);
      }
      // This message is not compressed.
      if (packetLimiter != null && !packetLimiter.account(in.readableBytes())) {
        throw new QuietDecoderException("Rate limit exceeded while processing packets for %s"
            .formatted(ctx.channel().remoteAddress()));
      }
      out.add(in.retain());
      return;
    }

    checkFrame(claimedUncompressedSize >= threshold, "Uncompressed size %s is less than"
        + " threshold %s", claimedUncompressedSize, threshold);
    if (direction == ProtocolUtils.Direction.CLIENTBOUND) {
      checkFrame(claimedUncompressedSize <= CLIENTBOUND_UNCOMPRESSED_CAP,
              "Uncompressed size %s exceeds hard threshold of %s", claimedUncompressedSize,
              CLIENTBOUND_UNCOMPRESSED_CAP);
    } else {
      checkFrame(claimedUncompressedSize <= SERVERBOUND_UNCOMPRESSED_CAP,
              "Uncompressed size %s exceeds hard threshold of %s", claimedUncompressedSize,
              SERVERBOUND_UNCOMPRESSED_CAP);
    }
    ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
    ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
    try {
      compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
      checkFrame(uncompressed.writerIndex() == claimedUncompressedSize,
              "Decompressed size %s does not match claimed uncompressed size %s", uncompressed.writerIndex(), claimedUncompressedSize);
      if (packetLimiter != null && !packetLimiter.account(claimedUncompressedSize)) {
        throw new QuietDecoderException("Rate limit exceeded while processing packets for %s"
            .formatted(ctx.channel().remoteAddress()));
      }
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    } finally {
      compatibleIn.release();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  public void setPacketLimiter(@Nullable PacketLimiter packetLimiter) {
    this.packetLimiter = packetLimiter;
  }
}
