package com.velocitypowered.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerChannelInitializerHolder implements Supplier<ChannelInitializer<Channel>> {

  private static final Logger LOGGER = LogManager.getLogger(ConnectionManager.class);
  private ChannelInitializer<Channel> initializer;

  public ServerChannelInitializerHolder(final ChannelInitializer<Channel> initializer) {
    this.initializer = initializer;
  }

  @Override
  public ChannelInitializer<Channel> get() {
    return this.initializer;
  }

  /**
   * Sets the channel initializer.
   * @param initializer the new initializer to use
   * @deprecated Internal implementation detail
   */
  @Deprecated
  public void set(final ChannelInitializer<Channel> initializer) {
    LOGGER.warn("The server channel initializer has been replaced by {}",
        Thread.currentThread().getStackTrace()[2]);
    this.initializer = initializer;
  }
}
