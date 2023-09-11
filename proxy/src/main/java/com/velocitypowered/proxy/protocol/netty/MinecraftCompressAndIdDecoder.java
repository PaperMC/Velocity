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

import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;
import static com.velocitypowered.proxy.protocol.util.NettyPreconditions.checkFrame;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.data.CompressedPacket;
import com.velocitypowered.proxy.protocol.netty.data.UncompressedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * Decompresses a Minecraft packet and decodes id.
 */
public class MinecraftCompressAndIdDecoder extends MessageToMessageDecoder<ByteBuf> {

  private int threshold;
  private final VelocityCompressor compressor;
  private final VelocityCompressor javaCompressor;

  /**
   * Constructs new Minecraft packet decompressor and id decoder.
   *
   * @param threshold Compression threshold.
   * @param compressor Preferred compressor.
   * @param javaCompressor Java compressor for partial decompression.
   */
  public MinecraftCompressAndIdDecoder(int threshold, VelocityCompressor compressor,
                                       VelocityCompressor javaCompressor) {
    this.threshold = threshold;
    this.compressor = compressor;
    this.javaCompressor = javaCompressor;
  }

  public MinecraftCompressAndIdDecoder() {
    this(0, null, null);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (threshold <= 0) {
      int originalReaderIndex = in.readerIndex();
      int packetId = ProtocolUtils.readVarInt(in);
      out.add(new UncompressedPacket(packetId, in.readerIndex(originalReaderIndex).retain()));
      return;
    }

    int claimedUncompressedSize = ProtocolUtils.readVarInt(in);
    if (claimedUncompressedSize == 0) {
      int originalReaderIndex = in.readerIndex();
      int packetId = ProtocolUtils.readVarInt(in);
      out.add(new UncompressedPacket(packetId, in.readerIndex(originalReaderIndex).retain()));
      return;
    }

    checkFrame(claimedUncompressedSize >= threshold, "Uncompressed size %s is less than"
        + " threshold %s", claimedUncompressedSize, threshold);

    ByteBuf packetIdBuf = preferredBuffer(ctx.alloc(), this.compressor, 5);
    int readerIndex = in.readerIndex();
    this.javaCompressor.inflatePartial(in, packetIdBuf, 5);
    in.readerIndex(readerIndex);
    int packetId = ProtocolUtils.readVarInt(packetIdBuf);
    packetIdBuf.release();

    out.add(new CompressedPacket(packetId, claimedUncompressedSize, in.retain(), this.compressor));
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    if (compressor != null) {
      compressor.close();
    }
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
