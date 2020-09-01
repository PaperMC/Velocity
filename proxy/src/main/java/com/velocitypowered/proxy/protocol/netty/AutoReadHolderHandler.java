package com.velocitypowered.proxy.protocol.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A variation on {@link io.netty.handler.flow.FlowControlHandler} that explicitly holds messages
 * on {@code channelRead} and only releases them on an explicit read operation.
 */
public class AutoReadHolderHandler extends ChannelDuplexHandler {

  private final Queue<Object> queuedMessages;

  public AutoReadHolderHandler() {
    this.queuedMessages = new ArrayDeque<>();
  }

  @Override
  public void read(ChannelHandlerContext ctx) throws Exception {
    drainQueuedMessages(ctx);
    ctx.read();
  }

  private void drainQueuedMessages(ChannelHandlerContext ctx) {
    if (!this.queuedMessages.isEmpty()) {
      Object queued;
      while ((queued = this.queuedMessages.poll()) != null) {
        ctx.fireChannelRead(queued);
      }
      ctx.fireChannelReadComplete();
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (ctx.channel().config().isAutoRead()) {
      ctx.fireChannelRead(msg);
    } else {
      this.queuedMessages.add(msg);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (this.queuedMessages.isEmpty()) {
      ctx.fireChannelReadComplete();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    for (Object message : this.queuedMessages) {
      ReferenceCountUtil.release(message);
    }
    this.queuedMessages.clear();
  }
}
