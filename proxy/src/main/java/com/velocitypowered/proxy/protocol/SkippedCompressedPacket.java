package com.velocitypowered.proxy.protocol;

import io.netty.buffer.ByteBuf;

public class SkippedCompressedPacket {

  public final int packetId;
  public final int packetLength;
  public final ByteBuf buffer;

  /**
   * Creates a wrapper for compressed Minecraft packet.
   *
   * @param packetId     - decompressed id of the packet
   * @param packetLength - uncompressed packet size
   * @param buffer       - compressed packet data
   */
  public SkippedCompressedPacket(int packetId, int packetLength, ByteBuf buffer) {
    this.packetId = packetId;
    this.packetLength = packetLength;
    this.buffer = buffer;
  }
}
