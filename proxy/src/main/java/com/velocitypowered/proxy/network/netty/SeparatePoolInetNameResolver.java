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

package com.velocitypowered.proxy.network.netty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@code InetNameResolver} that performs blocking DNS name lookups
 * in a separate thread, avoiding blocking the Netty threads for an extended period of time
 * and without the downsides of Netty's native DNS resolver.
 */
public final class SeparatePoolInetNameResolver extends InetNameResolver {

  private final ExecutorService resolveExecutor;
  private final InetNameResolver delegate;
  private final Cache<String, List<InetAddress>> cache;
  private final Cache<String, Boolean> negativeCache;
  private AddressResolverGroup<InetSocketAddress> resolverGroup;

  /**
   * Creates a new instance of {@code SeparatePoolInetNameResolver}.
   *
   * @param executor the {@link EventExecutor} which is used to notify the listeners of the
   *                 {@link Future} returned by {@link #resolve(String)}
   * @param threadCount the number of threads in the DNS resolver pool
   * @param cacheTtlSeconds the positive DNS cache TTL, in seconds
   * @param negativeCacheTtlSeconds the negative DNS cache TTL, in seconds ({@code 0} disables it)
   */
  public SeparatePoolInetNameResolver(final EventExecutor executor, final int threadCount,
      final int cacheTtlSeconds, final int negativeCacheTtlSeconds) {
    super(executor);
    this.resolveExecutor = Executors.newFixedThreadPool(Math.max(1, threadCount),
        new ThreadFactoryBuilder()
            .setNameFormat("Velocity DNS Resolver #%d")
            .setDaemon(true)
            .build());
    this.delegate = new DefaultNameResolver(executor);
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(Math.max(0, cacheTtlSeconds), TimeUnit.SECONDS)
        .build();
    // bVelocity: a short-lived negative cache stops a dead/misconfigured backend hostname from
    // being re-queried on every connection attempt. Without it, a failing lookup (which can block
    // for the system DNS timeout) re-occupies a resolver thread each time a player tries to join.
    this.negativeCache = negativeCacheTtlSeconds > 0
        ? Caffeine.newBuilder()
            .expireAfterWrite(negativeCacheTtlSeconds, TimeUnit.SECONDS)
            .build()
        : null;
  }

  @Override
  protected void doResolve(String inetHost, Promise<InetAddress> promise) throws Exception {
    List<InetAddress> addresses = cache.getIfPresent(inetHost);
    if (addresses != null) {
      promise.trySuccess(addresses.getFirst());
      return;
    }
    if (isNegativelyCached(inetHost, promise)) {
      return;
    }

    try {
      resolveExecutor.execute(() -> {
        // bVelocity: cache the FULL address list and resolve to its first element. Caching only
        // the single resolved address (as the previous code did) pollutes doResolveAll's cache
        // with a 1-element list, silently dropping the rest of the RR-set and breaking round-robin
        // over multi-A/AAAA backends. Delegate to resolveAll so both paths share one cache shape.
        final Promise<List<InetAddress>> allPromise = executor().newPromise();
        allPromise.addListener(future -> {
          if (future.isSuccess()) {
            List<InetAddress> all = (List<InetAddress>) future.getNow();
            cache.put(inetHost, all);
            promise.trySuccess(all.getFirst());
          } else if (future.isCancelled()) {
            // bVelocity: a cancelled lookup (e.g. the connecting player disconnected mid-resolve)
            // is not a DNS failure — do not poison the negative cache, which would otherwise lock
            // every other player out of this backend for the negative TTL.
            promise.tryFailure(future.cause());
          } else {
            recordNegative(inetHost);
            promise.tryFailure(future.cause());
          }
        });
        this.delegate.resolveAll(inetHost, allPromise);
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
    if (isNegativelyCached(inetHost, promise)) {
      return;
    }

    try {
      promise.addListener(future -> {
        if (future.isSuccess()) {
          cache.put(inetHost, (List<InetAddress>) future.getNow());
        } else if (future.isCancelled()) {
          // bVelocity: see doResolve — a cancellation (player disconnect mid-resolve) is not a DNS
          // failure and must not pollute the negative cache.
        } else {
          recordNegative(inetHost);
        }
      });
      resolveExecutor.execute(() -> this.delegate.resolveAll(inetHost, promise));
    } catch (RejectedExecutionException e) {
      promise.setFailure(e);
    }
  }

  /**
   * If the host is in the negative cache, fails the promise with a fresh UnknownHostException and
   * returns {@code true}.
   *
   * @param inetHost the hostname to check
   * @param promise the promise to fail on a negative-cache hit
   * @return {@code true} if the promise was completed (negative cache hit)
   */
  private boolean isNegativelyCached(final String inetHost, final Promise<?> promise) {
    if (this.negativeCache != null && this.negativeCache.getIfPresent(inetHost) != null) {
      promise.tryFailure(new UnknownHostException("Negative cache hit for " + inetHost));
      return true;
    }
    return false;
  }

  /**
   * Records a failed lookup in the negative cache, if enabled.
   *
   * @param inetHost the hostname that failed to resolve
   */
  private void recordNegative(final String inetHost) {
    if (this.negativeCache != null) {
      this.negativeCache.put(inetHost, Boolean.TRUE);
    }
  }

  /**
   * Shuts down the resolver thread pool and releases the lazily-created {@link AddressResolverGroup}.
   */
  public void shutdown() {
    this.resolveExecutor.shutdown();
    // bVelocity: the lazily-created AddressResolverGroup caches per-executor AddressResolver
    // instances; close it so those (and their references) are released on shutdown / reload.
    if (this.resolverGroup != null) {
      this.resolverGroup.close();
    }
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
