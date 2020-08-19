package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.SkippedCompressedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class SkippedPacketWriter extends MessageToByteEncoder<SkippedCompressedPacket> {

  public static final SkippedPacketWriter INSTANCE = new SkippedPacketWriter();

  @Override
  protected void encode(ChannelHandlerContext ctx, SkippedCompressedPacket packet, ByteBuf out)
      throws Exception {
    try {
      ProtocolUtils.writeVarInt(out, packet.packetLength);
      out.writeBytes(packet.buffer);
    } finally {
      packet.buffer.release();
    }
  }
}
