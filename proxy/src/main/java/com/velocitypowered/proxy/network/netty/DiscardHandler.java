package com.velocitypowered.proxy.network.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

@Sharable
public class DiscardHandler extends ChannelInboundHandlerAdapter {

  public static final DiscardHandler HANDLER = new DiscardHandler();

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ReferenceCountUtil.release(msg);
  }
}
