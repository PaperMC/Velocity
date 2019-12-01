package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {

  private final ProtocolUtils.Direction direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftDecoder} decoding packets from the specified {@code direction}.
   *
   * @param direction the direction from which we decode from
   */
  public MinecraftDecoder(ProtocolUtils.Direction direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.registry = direction
        .getProtocolRegistry(StateRegistry.HANDSHAKE, ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    if (!msg.isReadable()) {
      return;
    }

    ByteBuf slice = msg.slice();

    int packetId = ProtocolUtils.readVarInt(msg);
    MinecraftPacket packet = this.registry.createPacket(packetId);
    if (packet == null) {
      msg.skipBytes(msg.readableBytes());
      out.add(slice.retain());
    } else {
      try {
        packet.decode(msg, direction, registry.version);
      } catch (Exception e) {
        throw new CorruptedFrameException(
            "Error decoding " + packet.getClass() + " " + getExtraConnectionDetail(packetId), e);
      }
      if (msg.isReadable()) {
        throw new CorruptedFrameException("Did not read full packet for " + packet.getClass() + " "
                + getExtraConnectionDetail(packetId));
      }
      out.add(packet);
    }
  }

  private String getExtraConnectionDetail(int packetId) {
    return "Direction " + direction + " Protocol " + registry.version + " State " + state
        + " ID " + Integer.toHexString(packetId);
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.registry = direction.getProtocolRegistry(state, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
