package com.velocitypowered.proxy.network.resolver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.InetNameResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class SeparatePoolInetNameResolver extends InetNameResolver {

  private final ExecutorService resolveExecutor;
  private final InetNameResolver delegate;
  private final Cache<String, List<InetAddress>> cache;
  private AddressResolverGroup<InetSocketAddress> resolverGroup;

  /**
   * Creates a new instance of {@code SeparatePoolInetNameResolver}.
   *
   * @param executor the {@link EventExecutor} which is used to notify the listeners of the {@link
   *                 Future} returned by {@link #resolve(String)}
   */
  public SeparatePoolInetNameResolver(EventExecutor executor) {
    super(executor);
    this.resolveExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setNameFormat("Velocity DNS Resolver")
            .setDaemon(true)
            .build());
    this.delegate = new DefaultNameResolver(executor);
    this.cache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();
  }

  @Override
  protected void doResolve(String inetHost, Promise<InetAddress> promise) throws Exception {
    List<InetAddress> addresses = cache.getIfPresent(inetHost);
    if (addresses != null) {
      promise.trySuccess(addresses.get(0));
      return;
    }

    try {
      resolveExecutor.execute(() -> {
        promise.addListener(future -> {
          if (future.isSuccess()) {
            cache.put(inetHost, ImmutableList.of((InetAddress) future.getNow()));
          }
        });
        this.delegate.resolve(inetHost, promise);
      });
    } catch (RejectedExecutionException e) {
      promise.setFailure(e);
    }
  }

  @Override
  protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise)
      throws Exception {
    List<InetAddress> addresses = cache.getIfPresent(inetHost);
    if (addresses != null) {
      promise.trySuccess(addresses);
      return;
    }

    try {
      promise.addListener(future -> {
        if (future.isSuccess()) {
          cache.put(inetHost, (List<InetAddress>) future.getNow());
        }
      });
      resolveExecutor.execute(() -> this.delegate.resolveAll(inetHost, promise));
    } catch (RejectedExecutionException e) {
      promise.setFailure(e);
    }
  }

  public void shutdown() {
    this.resolveExecutor.shutdown();
  }

  /**
   * Returns a view of this resolver as a AddressResolverGroup.
   *
   * @return a view of this resolver as a AddressResolverGroup
   */
  public AddressResolverGroup<InetSocketAddress> asGroup() {
    if (this.resolverGroup == null) {
      this.resolverGroup = new AddressResolverGroup<InetSocketAddress>() {
        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
          return asAddressResolver();
        }
      };
    }
    return this.resolverGroup;
  }
}
