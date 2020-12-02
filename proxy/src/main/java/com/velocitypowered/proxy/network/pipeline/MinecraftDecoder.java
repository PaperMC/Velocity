package com.velocitypowered.proxy.network.pipeline;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.StateRegistry;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;

public class MinecraftDecoder extends ChannelInboundHandlerAdapter {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietDecoderException DECODE_FAILED =
      new QuietDecoderException("A packet did not decode successfully (invalid data). If you are a "
          + "developer, launch Velocity with -Dvelocity.packet-decode-logging=true to see more.");

  private final PacketDirection direction;
  private StateRegistry state;
  private StateRegistry.PacketRegistry.ProtocolRegistry registry;

  /**
   * Creates a new {@code MinecraftDecoder} decoding packets from the specified {@code direction}.
   *
   * @param direction the direction from which we decode from
   */
  public MinecraftDecoder(PacketDirection direction) {
    this.direction = Preconditions.checkNotNull(direction, "direction");
    this.registry = StateRegistry.HANDSHAKE.getProtocolRegistry(direction,
        ProtocolVersion.MINIMUM_VERSION);
    this.state = StateRegistry.HANDSHAKE;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      ByteBuf buf = (ByteBuf) msg;
      tryDecode(ctx, buf);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void tryDecode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
    if (!ctx.channel().isActive()) {
      buf.release();
      return;
    }

    int originalReaderIndex = buf.readerIndex();
    int packetId = ProtocolUtils.readVarInt(buf);
    Packet packet = null;
    try {
      packet = this.registry.readPacket(packetId, buf, direction, registry.version);
    } catch (Exception e) {
      throw handleDecodeFailure(e, packet, packetId); // TODO: packet is always null
    }
    if (packet == null) {
      buf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(buf);
    } else {
      try {
        if (buf.isReadable()) {
          throw handleNotReadEnough(packet, packetId);
        }
        ctx.fireChannelRead(packet);
      } finally {
        buf.release();
      }
    }
  }

  private Exception handleNotReadEnough(Packet packet, int packetId) {
    if (DEBUG) {
      return new CorruptedFrameException("Did not read full packet for " + packet.getClass() + " "
          + getExtraConnectionDetail(packetId));
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleDecodeFailure(Exception cause, Packet packet, int packetId) {
    if (DEBUG) {
      return new CorruptedFrameException(
          "Error decoding " + packet.getClass() + " " + getExtraConnectionDetail(packetId), cause);
    } else {
      return DECODE_FAILED;
    }
  }

  private String getExtraConnectionDetail(int packetId) {
    return "Direction " + direction + " Protocol " + registry.version + " State " + state
        + " ID " + Integer.toHexString(packetId);
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.registry = state.getProtocolRegistry(direction, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
