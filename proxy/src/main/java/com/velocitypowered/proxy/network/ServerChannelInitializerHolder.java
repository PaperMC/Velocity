package com.velocitypowered.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

final class ServerChannelInitializerHolder extends ChannelInitializerHolder<Channel> {

  ServerChannelInitializerHolder(final ChannelInitializer<Channel> initializer) {
    super("server channel", initializer);
  }
}
