package com.velocitypowered.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

final class BackendChannelInitializerHolder extends ChannelInitializerHolder<Channel> {

  BackendChannelInitializerHolder(final ChannelInitializer<Channel> initializer) {
    super("backend channel", initializer);
  }
}
