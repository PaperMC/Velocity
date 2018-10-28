package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {

  private final ProtocolConstants.Direction direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolVersion protocolVersion;

  public MinecraftDecoder(ProtocolConstants.Direction direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.protocolVersion = direction
        .getProtocol(StateRegistry.HANDSHAKE, ProtocolConstants.MINIMUM_GENERIC_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    if (!msg.isReadable()) {
      return;
    }

    ByteBuf slice = msg.slice();

    int packetId = ProtocolUtils.readVarInt(msg);
    MinecraftPacket packet = this.protocolVersion.createPacket(packetId);
    if (packet == null) {
      msg.skipBytes(msg.readableBytes());
      out.add(slice.retain());
    } else {
      try {
        packet.decode(msg, direction, protocolVersion.version);
      } catch (Exception e) {
        throw new CorruptedFrameException(
            "Error decoding " + packet.getClass() + " Direction " + direction
                + " Protocol " + protocolVersion.version + " State " + state + " ID " + Integer
                .toHexString(packetId), e);
      }
      if (msg.isReadable()) {
        throw new CorruptedFrameException(
            "Did not read full packet for " + packet.getClass() + " Direction " + direction
                + " Protocol " + protocolVersion.version + " State " + state + " ID " + Integer
                .toHexString(packetId));
      }
      out.add(packet);
    }
  }

  public void setProtocolVersion(int protocolVersion) {
    this.protocolVersion = direction.getProtocol(state, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(protocolVersion.version);
  }
}
