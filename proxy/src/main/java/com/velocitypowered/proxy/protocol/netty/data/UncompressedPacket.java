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

package com.velocitypowered.proxy.protocol.netty.data;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.util.zip.DataFormatException;

/**
 * Binary uncompressed identified packet.
 */
public class UncompressedPacket extends IdentifiedPacket {

  private final ByteBuf packetBuf;

  public UncompressedPacket(int packetId, ByteBuf packetBuf) {
    super(packetId);
    this.packetBuf = packetBuf;
  }

  /**
   * Allocates new buffer and compresses current packet buffer to it.
   *
   * @param compressor Compressor
   * @param allocator Buffer allocator
   * @return Allocated buffer
   * @throws DataFormatException Error occurred during compression.
   */
  public ByteBuf compress(VelocityCompressor compressor, ByteBufAllocator allocator)
      throws DataFormatException {
    ByteBuf compressed = preferredBuffer(allocator, compressor, 256);
    try {
      return compress(compressor, allocator, compressed);
    } catch (DataFormatException e) {
      compressed.release();
      throw e;
    }
  }

  /**
   * Compresses packet buffer.
   *
   * @param compressor Compressor.
   * @param allocator Buffer allocator.
   * @param compressed Target buffer.
   * @return Compressed buffer.
   * @throws DataFormatException Error occurred during compression.
   */
  public ByteBuf compress(VelocityCompressor compressor, ByteBufAllocator allocator,
                          ByteBuf compressed) throws DataFormatException {
    ByteBuf compatibleIn = ensureCompatible(allocator, compressor, this.packetBuf.duplicate());

    int originalWriterIndex = compressed.writerIndex();
    try {
      compressor.deflate(compatibleIn, compressed);
    } finally {
      compatibleIn.release();
    }
    int compressedLength = compressed.writerIndex() - originalWriterIndex;
    if (compressedLength >= 1 << 21) {
      throw new DataFormatException("Compressed packet is very large (over 2MiB).");
    }
    return compressed;
  }

  public ByteBuf getPacketBuf() {
    return this.packetBuf;
  }
}
