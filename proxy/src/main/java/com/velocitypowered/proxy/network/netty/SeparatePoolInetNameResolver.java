/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.network.netty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty5.resolver.AddressResolver;
import io.netty5.resolver.AddressResolverGroup;
import io.netty5.resolver.DefaultNameResolver;
import io.netty5.resolver.InetNameResolver;
import io.netty5.util.concurrent.EventExecutor;
import io.netty5.util.concurrent.Future;
import io.netty5.util.concurrent.Promise;
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

    resolveExecutor.execute(() -> this.delegate.resolve(inetHost)
        .addListener(future -> {
          if (future.isSuccess()) {
            cache.put(inetHost, ImmutableList.of(future.getNow()));
            promise.trySuccess(future.getNow());
          } else {
            promise.tryFailure(future.cause());
          }
        }));
  }

  @Override
  protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise)
      throws Exception {
    List<InetAddress> addresses = cache.getIfPresent(inetHost);
    if (addresses != null) {
      promise.trySuccess(addresses);
      return;
    }

    resolveExecutor.execute(() -> this.delegate.resolve(inetHost)
        .addListener(future -> {
          if (future.isSuccess()) {
            List<InetAddress> addressList = ImmutableList.of(future.getNow());
            cache.put(inetHost, addressList);
            promise.trySuccess(addressList);
          } else {
            promise.tryFailure(future.cause());
          }
        }));
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
