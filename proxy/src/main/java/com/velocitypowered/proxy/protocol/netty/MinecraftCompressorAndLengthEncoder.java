/*
 * Copyright (C) 2018 Velocity Contributors
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

public class MinecraftCompressorAndLengthEncoder extends MessageToByteEncoder<ByteBuf> {

  private static final boolean MUST_USE_SAFE_AND_SLOW_COMPRESSION_HANDLING =
      Boolean.getBoolean("velocity.increased-compression-cap");

  private int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressorAndLengthEncoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    int uncompressed = msg.readableBytes();
    if (uncompressed < threshold) {
      // Under the threshold, there is nothing to do.
      ProtocolUtils.writeVarInt(out, uncompressed + 1);
      ProtocolUtils.writeVarInt(out, 0);
      out.writeBytes(msg);
    } else {
      if (MUST_USE_SAFE_AND_SLOW_COMPRESSION_HANDLING) {
        handleCompressedSafe(ctx, msg, out);
      } else {
        handleCompressedFast(ctx, msg, out);
      }
    }
  }

  private void handleCompressedFast(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
      throws DataFormatException {
    int uncompressed = msg.readableBytes();

    ProtocolUtils.write21BitVarInt(out, 0); // Dummy packet length
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
      throw new DataFormatException("The server sent a very large (over 2MiB compressed) packet. "
          + "Please restart Velocity with the JVM flag -Dvelocity.increased-compression-cap=true "
          + "to fix this issue.");
    }

    int writerIndex = out.writerIndex();
    int packetLength = out.readableBytes() - 3;
    out.writerIndex(0);
    ProtocolUtils.write21BitVarInt(out, packetLength); // Rewrite packet length
    out.writerIndex(writerIndex);
  }

  private void handleCompressedSafe(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
      throws DataFormatException {
    int uncompressed = msg.readableBytes();
    ByteBuf tmpBuf = MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, uncompressed - 1);
    try {
      ProtocolUtils.writeVarInt(tmpBuf, uncompressed);
      ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, msg);
      try {
        compressor.deflate(compatibleIn, tmpBuf);
      } finally {
        compatibleIn.release();
      }

      ProtocolUtils.writeVarInt(out, tmpBuf.readableBytes());
      out.writeBytes(tmpBuf);
    } finally {
      tmpBuf.release();
    }
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
