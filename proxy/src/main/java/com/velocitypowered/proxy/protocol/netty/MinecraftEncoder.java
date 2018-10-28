package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftEncoder extends MessageToByteEncoder<MinecraftPacket> {

  private final ProtocolConstants.Direction direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolVersion protocolVersion;

  public MinecraftEncoder(ProtocolConstants.Direction direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.protocolVersion = direction
        .getProtocol(StateRegistry.HANDSHAKE, ProtocolConstants.MINIMUM_GENERIC_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, MinecraftPacket msg, ByteBuf out) {
    int packetId = this.protocolVersion.getPacketId(msg);
    ProtocolUtils.writeVarInt(out, packetId);
    msg.encode(out, direction, protocolVersion.version);
  }

  public void setProtocolVersion(final int protocolVersion) {
    this.protocolVersion = direction.getProtocol(state, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(protocolVersion.version);
  }
}
