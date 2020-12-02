package com.velocitypowered.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChannelInitializerHolder<C extends Channel> implements Supplier<ChannelInitializer<C>> {
  private static final Logger LOGGER = LogManager.getLogger(ChannelInitializerHolder.class);
  private final String name;
  private ChannelInitializer<C> initializer;

  ChannelInitializerHolder(final String name, final ChannelInitializer<C> initializer) {
    this.name = name;
    this.initializer = initializer;
  }

  @Override
  public ChannelInitializer<C> get() {
    return this.initializer;
  }

  /**
   * Sets the channel initializer.
   *
   * @param initializer the new initializer to use
   * @deprecated Internal implementation detail
   */
  @Deprecated
  public void set(final ChannelInitializer<C> initializer) {
    LOGGER.warn("The {} initializer has been replaced by {}", this.name,
        Thread.currentThread().getStackTrace()[2]);
    this.initializer = initializer;
  }
}
