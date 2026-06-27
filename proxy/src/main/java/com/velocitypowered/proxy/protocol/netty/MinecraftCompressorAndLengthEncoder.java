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

import static com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder.IS_JAVA_CIPHER;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.DataFormatException;
import top.notcoral.velocity.compression.BvCompressionStats;

/**
 * Handler for compressing Minecraft packets.
 */
public class MinecraftCompressorAndLengthEncoder extends MessageToByteEncoder<ByteBuf> {

  /**
   * Default headroom added on top of the uncompressed size when pre-allocating the
   * compressed-output buffer, used when no bVelocity configuration is available (e.g. tests).
   * Any conforming zlib stream is bounded by the deflate stored-block worst case (one 5-byte
   * block header per 65535 bytes, plus the 2-byte zlib header and 4-byte adler32 trailer); for
   * the dominant sub-64KiB Minecraft packet range that collapses to {@code input + 11}, so a
   * small fixed headroom pre-allocates enough that the compressor's grow-loop almost never has
   * to retry. A retry is expensive: libdeflate returns 0 and produces nothing when the
   * destination is too small, so the loop would otherwise discard a full compression pass and
   * recompress from scratch. The grow-loop remains in place as a backstop for the rare
   * large/incompressible case beyond this headroom.
   */
  static final int DEFAULT_COMPRESS_BOUND_HEADROOM = 16;

  private int threshold;
  private final VelocityCompressor compressor;
  private final int compressBoundHeadroom;
  private final boolean statsEnabled;

  /**
   * Creates a compressor encoder with default optimization settings.
   *
   * @param threshold the compression threshold
   * @param compressor the compressor instance
   */
  public MinecraftCompressorAndLengthEncoder(int threshold, VelocityCompressor compressor) {
    this(threshold, compressor, DEFAULT_COMPRESS_BOUND_HEADROOM, true);
  }

  /**
   * Creates a compressor encoder with bVelocity-configured optimization settings.
   *
   * @param threshold the compression threshold
   * @param compressor the compressor instance
   * @param compressBoundHeadroom headroom added when pre-allocating the output buffer
   * @param statsEnabled whether to collect per-packet compression statistics
   */
  public MinecraftCompressorAndLengthEncoder(int threshold, VelocityCompressor compressor,
      int compressBoundHeadroom, boolean statsEnabled) {
    this.threshold = threshold;
    this.compressor = compressor;
    this.compressBoundHeadroom = compressBoundHeadroom;
    this.statsEnabled = statsEnabled;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    int initialWriterIndex = out.writerIndex();
    int uncompressed = msg.readableBytes();
    if (uncompressed < threshold) {
      // Under the threshold, there is nothing to do.
      ProtocolUtils.writeVarInt(out, uncompressed + 1);
      out.writeByte(0);
      out.writeBytes(msg);
      if (statsEnabled) {
        // Wire size of a passthrough packet is fully determined by its uncompressed length
        // (length varint + 0x00 marker + payload), so no writerIndex re-read is needed here.
        BvCompressionStats.INSTANCE.recordPassThrough(
            uncompressed,
            ProtocolUtils.varIntBytes(uncompressed + 1) + 1 + uncompressed
        );
      }
    } else {
      handleCompressed(ctx, msg, out);
      if (statsEnabled) {
        BvCompressionStats.INSTANCE.recordCompressed(
            uncompressed,
            out.writerIndex() - initialWriterIndex
        );
      }
    }
  }

  private void handleCompressed(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
      throws DataFormatException {
    int uncompressed = msg.readableBytes();

    out.writeMedium(0); // Reserve the packet length
    ProtocolUtils.writeVarInt(out, uncompressed);
    ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);

    int startCompressed = out.writerIndex();
    try {
      compressor.deflate(compatibleIn, out);
    } finally {
      compatibleIn.release();
    }
    int compressedLength = out.writerIndex() - startCompressed;
    if (compressedLength >= 1 << 21) {
      throw new DataFormatException("The server sent a very large (over 2MiB compressed) packet.");
    }

    int packetLength = out.readableBytes() - 3;
    out.setMedium(0, ProtocolUtils.encode21BitVarInt(packetLength)); // Rewrite packet length
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    int uncompressed = msg.readableBytes();
    if (uncompressed < threshold) {
      int finalBufferSize = uncompressed + 1;
      finalBufferSize += ProtocolUtils.varIntBytes(finalBufferSize);
      return IS_JAVA_CIPHER
          ? ctx.alloc().heapBuffer(finalBufferSize)
          : ctx.alloc().directBuffer(finalBufferSize);
    }

    // (maximum data length after compression) + packet length varint + uncompressed data varint.
    // compressBoundHeadroom keeps the output large enough that the compressor's grow-loop
    // (which discards a full pass on insufficient room) almost never triggers.
    int initialBufferSize = uncompressed + compressBoundHeadroom + 3
        + ProtocolUtils.varIntBytes(uncompressed);
    return MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, initialBufferSize);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
