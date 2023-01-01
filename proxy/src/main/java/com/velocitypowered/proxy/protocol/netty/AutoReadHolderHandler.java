/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A variation on {@link io.netty.handler.flow.FlowControlHandler} that explicitly holds messages on
 * {@code channelRead} and only releases them on an explicit read operation.
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
    if (ctx.channel().config().isAutoRead()) {
      if (!this.queuedMessages.isEmpty()) {
        this.drainQueuedMessages(ctx); // will also call fireChannelReadComplete()
      } else {
        ctx.fireChannelReadComplete();
      }
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
