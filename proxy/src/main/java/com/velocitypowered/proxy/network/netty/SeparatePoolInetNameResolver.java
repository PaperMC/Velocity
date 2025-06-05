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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.InetNameResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * An optimized implementation of {@code InetNameResolver} that performs DNS lookups in a
 * dedicated thread pool. This design prevents blocking Netty's event loop threads while
 * maintaining high throughput and consistency.
 *
 * <p>Key features:
 * <ul>
 *   <li>Request coalescing: Concurrent requests for the same host are combined</li>
 *   <li>Unified caching: Full DNS results are cached and reused across resolve types</li>
 *   <li>Configurable threading: Thread pool size adjustable via system property</li>
 *   <li>Graceful shutdown: Ensures completion of in-flight requests during shutdown</li>
 * </ul>
 */
public final class SeparatePoolInetNameResolver extends InetNameResolver {

  /**
   * System property to configure DNS resolver thread count.
   * Default value: 2 threads.
   */
  private static final int DNS_RESOLVER_THREADS = Integer.getInteger(
      "velocity.dns_resolver_threads", 2
  );

  private final ExecutorService resolveExecutor;
  private final InetNameResolver delegate;
  private final Cache<String, List<InetAddress>> cache;
  
  /**
   * Tracks in-flight DNS resolutions to prevent duplicate requests.
   * Key: Hostname being resolved
   * Value: Promise representing the ongoing resolution
   */
  private final ConcurrentHashMap<String, Promise<List<InetAddress>>> pendingResolutions;
  
  private AddressResolverGroup<InetSocketAddress> resolverGroup;

  /**
   * Creates a new optimized DNS resolver instance.
   *
   * @param executor the {@link EventExecutor} used to notify resolution listeners
   */
  public SeparatePoolInetNameResolver(EventExecutor executor) {
    super(executor);
    this.resolveExecutor = Executors.newFixedThreadPool(
        DNS_RESOLVER_THREADS,
        new ThreadFactoryBuilder()
            .setNameFormat("Velocity DNS Resolver #%d")
            .setDaemon(true)
            .build()
    );
    this.delegate = new DefaultNameResolver(executor);
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();
    this.pendingResolutions = new ConcurrentHashMap<>();
  }

  @Override
  protected void doResolve(String inetHost, Promise<InetAddress> promise) throws Exception {
    resolveWithFullList(inetHost, promise, (addresses, resultPromise) -> {
      if (!addresses.isEmpty()) {
        resultPromise.trySuccess(addresses.get(0));
      } else {
        resultPromise.tryFailure(new Exception("No addresses found for " + inetHost));
      }
    });
  }

  @Override
  protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise) throws Exception {
    resolveWithFullList(inetHost, promise, (addresses, resultPromise) -> {
      resultPromise.trySuccess(addresses);
    });
  }

  /**
   * Core resolution method handling cache lookup, request coalescing and async execution.
   *
   * @param inetHost hostname to resolve
   * @param promise resolution promise to fulfill
   * @param callback processing strategy for resolved addresses
   * @param <T> resolution result type (single address or list)
   */
  private <T> void resolveWithFullList(
      String inetHost,
      Promise<T> promise,
      ResolutionCallback<T> callback
  ) {
    // Check cache first
    List<InetAddress> cached = cache.getIfPresent(inetHost);
    if (cached != null) {
      callback.onResolution(cached, promise);
      return;
    }

    // Check for existing resolution in progress
    Promise<List<InetAddress>> pending = pendingResolutions.get(inetHost);
    if (pending != null) {
      pending.addListener((Future<List<InetAddress>> future) -> {
        if (future.isSuccess()) {
          callback.onResolution(future.getNow(), promise);
        } else {
          promise.tryFailure(future.cause());
        }
      });
      return;
    }

    // Initiate new resolution
    try {
      final Promise<List<InetAddress>> newPromise = executor().newPromise();
      if (pendingResolutions.putIfAbsent(inetHost, newPromise) != null) {
        // Another thread started resolution concurrently, retry
        doResolve(inetHost, promise);
        return;
      }

      resolveExecutor.execute(() -> {
        // Always resolve full list for caching consistency
        delegate.resolveAll(inetHost, newPromise);
      });

      newPromise.addListener((Future<List<InetAddress>> future) -> {
        // Cache result and clean up
        pendingResolutions.remove(inetHost);
        if (future.isSuccess()) {
          List<InetAddress> result = future.getNow();
          cache.put(inetHost, result);
          callback.onResolution(result, promise);
        } else {
          promise.tryFailure(future.cause());
        }
      });
    } catch (RejectedExecutionException e) {
      pendingResolutions.remove(inetHost);
      promise.tryFailure(e);
    }
  }

  /**
   * Shuts down the resolver with graceful termination.
   * <p>Sequence:
   * <ol>
   *   <li>Prevent new tasks from being submitted</li>
   *   <li>Wait 5 seconds for existing tasks to complete</li>
   *   <li>Forcefully terminate if timeout is reached</li>
   * </ol>
   */
  public void shutdown() {
    resolveExecutor.shutdown();
    try {
      if (!resolveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        resolveExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      resolveExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Provides this resolver as an AddressResolverGroup.
   *
   * @return resolver group instance
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

  /**
   * Functional interface for processing resolved addresses.
   *
   * @param <T> result type (InetAddress or List<InetAddress>)
   */
  @FunctionalInterface
  private interface ResolutionCallback<T> {
    void onResolution(List<InetAddress> addresses, Promise<T> resultPromise);
  }
}
