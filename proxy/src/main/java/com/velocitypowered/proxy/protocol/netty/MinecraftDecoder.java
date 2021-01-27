package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;

public class MinecraftDecoder extends ChannelInboundHandlerAdapter {

  public static final boolean DEBUG = Boolean.getBoolean("velocity.packet-decode-logging");
  private static final QuietRuntimeException DECODE_FAILED =
      new QuietRuntimeException("A packet did not decode successfully (invalid data). If you are a "
          + "developer, launch Velocity with -Dvelocity.packet-decode-logging=true to see more.");

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
    this.registry = direction.getProtocolRegistry(StateRegistry.HANDSHAKE,
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
    MinecraftPacket packet = this.registry.createPacket(packetId);
    if (packet == null) {
      buf.readerIndex(originalReaderIndex);
      ctx.fireChannelRead(buf);
    } else {
      try {
        doLengthSanityChecks(buf, packet);

        try {
          packet.decode(buf, direction, registry.version);
        } catch (Exception e) {
          throw handleDecodeFailure(e, packet, packetId);
        }

        if (buf.isReadable()) {
          throw handleOverflow(packet, buf.readerIndex(), buf.writerIndex());
        }
        ctx.fireChannelRead(packet);
      } finally {
        buf.release();
      }
    }
  }

  private void doLengthSanityChecks(ByteBuf buf, MinecraftPacket packet) throws Exception {
    int expectedMinLen = packet.expectedMinLength(buf, direction, registry.version);
    int expectedMaxLen = packet.expectedMaxLength(buf, direction, registry.version);
    if (expectedMaxLen != -1 && buf.readableBytes() > expectedMaxLen) {
      throw handleOverflow(packet, expectedMaxLen, buf.readableBytes());
    }
    if (buf.readableBytes() < expectedMinLen) {
      throw handleUnderflow(packet, expectedMaxLen, buf.readableBytes());
    }
  }

  private Exception handleOverflow(MinecraftPacket packet, int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "big (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleUnderflow(MinecraftPacket packet, int expected, int actual) {
    if (DEBUG) {
      return new CorruptedFrameException("Packet sent for " + packet.getClass() + " was too "
          + "small (expected " + expected + " bytes, got " + actual + " bytes)");
    } else {
      return DECODE_FAILED;
    }
  }

  private Exception handleDecodeFailure(Exception cause, MinecraftPacket packet, int packetId) {
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
    this.registry = direction.getProtocolRegistry(state, protocolVersion);
  }

  public void setState(StateRegistry state) {
    this.state = state;
    this.setProtocolVersion(registry.version);
  }
}
