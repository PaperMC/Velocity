package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

/**
 * Queues up any pending PLAY packets while the client is in the CONFIG state.
 * <p>
 * Much of the Velocity API (i.e. chat messages) utilize PLAY packets, however
 * the client is incapable of receiving these packets during the CONFIG state.
 * Certain events such as the ServerPreConnectEvent may be called during this
 * time, and we need to ensure that any API that uses these packets will work
 * as expected.
 * <p>
 * This handler will queue up any packets that are sent to the client during
 * this time, and send them once the client has (re)entered the PLAY state.
 */
public class PlayPacketQueueHandler extends ChannelDuplexHandler {

  private final StateRegistry.PacketRegistry.ProtocolRegistry registry;
  private final Queue<MinecraftPacket> queue = PlatformDependent.newMpscQueue();

  public PlayPacketQueueHandler(ProtocolVersion version) {
    this.registry = StateRegistry.CONFIG.getProtocolRegistry(ProtocolUtils.Direction.CLIENTBOUND, version);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof MinecraftPacket)) {
      ctx.write(msg, promise);
      return;
    }

    // If the packet exists in the CONFIG state, we want to always
    // ensure that it gets sent out to the client
    if (this.registry.containsPacket(((MinecraftPacket) msg))) {
      ctx.write(msg, promise);
      return;
    }

    // Otherwise, queue the packet
    this.queue.offer((MinecraftPacket) msg);
  }

  @Override
  public void channelInactive(@NotNull ChannelHandlerContext ctx) {
    this.releaseQueue(ctx, false);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    this.releaseQueue(ctx, ctx.channel().isActive());
  }

  private void releaseQueue(ChannelHandlerContext ctx, boolean active) {
    if (this.queue.isEmpty()) {
      return;
    }

    // Send out all the queued packets
    MinecraftPacket packet;
    while ((packet = this.queue.poll()) != null) {
      if (active) {
        ctx.writeAndFlush(packet);
      } else {
        ReferenceCountUtil.release(packet);
      }
    }

    if (active) {
      ctx.flush();
    }
  }
}
