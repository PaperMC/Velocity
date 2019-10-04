package com.velocitypowered.proxy.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import io.netty.util.concurrent.FastThreadLocalThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class VelocityNettyThreadFactory implements ThreadFactory {

  private final AtomicInteger threadNumber = new AtomicInteger();
  private final String nameFormat;

  public VelocityNettyThreadFactory(String nameFormat) {
    this.nameFormat = checkNotNull(nameFormat, "nameFormat");
  }

  @Override
  public Thread newThread(Runnable r) {
    String name = String.format(nameFormat, threadNumber.getAndIncrement());
    return new FastThreadLocalThread(r, name);
  }
}
