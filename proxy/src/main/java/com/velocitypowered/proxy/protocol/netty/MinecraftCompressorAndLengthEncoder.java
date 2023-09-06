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
import com.velocitypowered.proxy.protocol.netty.data.CompressedPacket;
import com.velocitypowered.proxy.protocol.netty.data.IdentifiedPacket;
import com.velocitypowered.proxy.protocol.netty.data.UncompressedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.zip.DataFormatException;

/**
 * Handler for compressing Minecraft packets.
 */
public class MinecraftCompressorAndLengthEncoder extends MessageToByteEncoder<IdentifiedPacket> {

  private int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressorAndLengthEncoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, IdentifiedPacket msg, ByteBuf out)
      throws Exception {
    if (msg instanceof UncompressedPacket) {
      UncompressedPacket uncompressed = (UncompressedPacket) msg;
      int uncompressedLength = uncompressed.getPacketBuf().readableBytes();
      if (uncompressedLength < threshold || threshold <= 0) {
        // Under the threshold, there is nothing to do.
        ProtocolUtils.writeVarInt(out, uncompressedLength + 1);
        ProtocolUtils.writeVarInt(out, 0);
        out.writeBytes(uncompressed.getPacketBuf());
        uncompressed.getPacketBuf().release();
      } else {
        handleCompressed(ctx, uncompressed, out);
      }
    } else if (msg instanceof CompressedPacket) {
      CompressedPacket compressed = (CompressedPacket) msg;
      if (compressed.getUncompressedLength() < threshold || threshold <= 0) {
        ProtocolUtils.writeVarInt(out, compressed.getUncompressedLength() + 1);
        ProtocolUtils.writeVarInt(out, 0);
        ByteBuf decompressed = compressed.decompress(ctx.alloc());
        out.writeBytes(decompressed);
        decompressed.release();
      } else {
        ProtocolUtils.writeVarInt(out, compressed.getCompressedBuf().readableBytes()
            + ProtocolUtils.varIntBytes(compressed.getUncompressedLength()));
        ProtocolUtils.writeVarInt(out, compressed.getUncompressedLength());
        out.writeBytes(compressed.getCompressedBuf());
        compressed.getCompressedBuf().release();
      }
    }
  }

  private void handleCompressed(ChannelHandlerContext ctx, UncompressedPacket msg, ByteBuf out)
      throws DataFormatException {
    int uncompressed = msg.getPacketBuf().readableBytes();

    ProtocolUtils.write21BitVarInt(out, 0); // Dummy packet length
    ProtocolUtils.writeVarInt(out, uncompressed);

    msg.compress(this.compressor, ctx.alloc(), out);

    int writerIndex = out.writerIndex();
    int packetLength = out.readableBytes() - 3;
    out.writerIndex(0);
    ProtocolUtils.write21BitVarInt(out, packetLength); // Rewrite packet length
    out.writerIndex(writerIndex);
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, IdentifiedPacket msg,
                                   boolean preferDirect) throws Exception {
    int uncompressed;
    if (msg instanceof UncompressedPacket) {
      uncompressed = ((UncompressedPacket) msg).getPacketBuf().readableBytes();
    } else if (msg instanceof CompressedPacket) {
      uncompressed = ((CompressedPacket) msg).getUncompressedLength();
    } else {
      throw new IllegalArgumentException("Unsupported identified packet type.");
    }
    if (uncompressed < threshold) {
      int finalBufferSize = uncompressed + 1;
      finalBufferSize += ProtocolUtils.varIntBytes(finalBufferSize);
      return IS_JAVA_CIPHER
          ? ctx.alloc().heapBuffer(finalBufferSize)
          : ctx.alloc().directBuffer(finalBufferSize);
    }

    // (maximum data length after compression) + packet length varint + uncompressed data varint
    int initialBufferSize = (uncompressed - 1) + 3 + ProtocolUtils.varIntBytes(uncompressed);
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
