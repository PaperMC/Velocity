/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocityctd.proxy.redis.handler;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.velocityctd.proxy.redis.data.VelocityGetPlayerPing;
import com.velocityctd.proxy.redis.data.VelocityReload;
import com.velocityctd.proxy.redis.data.VelocityTransferRemote;
import com.velocityctd.proxy.redis.data.VelocityUptime;
import com.velocityctd.proxy.redis.transaction.TransactionData;
import com.velocityctd.proxy.redis.transaction.TransactionHandler;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Registry that holds all {@link TransactionHandler} entries for the VelocityRedis module.
 *
 * <p>This registry is used to register the 'handle' section of transactions only. The
 * completing and timeout behaviours are processed in the
 * {@link com.velocityctd.proxy.redis.transaction.Transaction Transaction} class itself.</p>
 */
public enum TransactionHandlerRegistry {

  /**
   * Handles the {@link VelocityGetPlayerPing} transaction by replying with the player's ping.
   */
  VELOCITY_GET_PLAYER_PING(VelocityGetPlayerPing.class, (server, data) -> {
    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      return null;
    }

    return completedFuture(player.getPing());
  }),

  /**
   * Handles the {@link VelocityUptime} transaction by replying with the proxy's uptime.
   */
  VELOCITY_UPTIME(VelocityUptime.class, (server, data) -> {
    if (!data.proxyId().equalsIgnoreCase(server.getProxyId())) {
      return null;
    }

    return completedFuture((System.currentTimeMillis() - server.getStartTime()) / 1000);
  }),

  /**
   * Handles the {@link VelocityReload} transaction by reloading the proxy's configuration.
   */
  VELOCITY_RELOAD(VelocityReload.class, (server, data) -> {
    if (!data.proxyId().equalsIgnoreCase(server.getProxyId())) {
      return null;
    }

    try {
      final boolean success = server.reloadConfiguration();
      if (success) {
        server.getLogger().info("Reloaded Velocity configuration on remote request.");
      } else {
        server.getLogger().error("Failed to reload Velocity configuration on remote request!");
      }
      return completedFuture(success);
    } catch (Exception e) {
      server.getLogger().error("Failed to reload Velocity configuration on remote request!", e);
      return completedFuture(false);
    }
  }),

  /**
   * Handles the {@link VelocityTransferRemote} transaction by transferring a player to another remote/proxy.
   */
  VELOCITY_TRANSFER_REMOTE(VelocityTransferRemote.class, (server, data) -> {
    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      return null;
    }

    if (player.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
      return completedFuture(false);
    }

    CompletableFuture<Boolean> fut = new CompletableFuture<>();
    server.getScheduler().buildTask(VelocityVirtualPlugin.INSTANCE, () -> {
      player.transferToHost(new InetSocketAddress(data.ip(), data.port()));
      fut.complete(true);
    }).delay(100, TimeUnit.MILLISECONDS).schedule();

    return fut;
  }),
  ;

  /**
   * The data class handled by this transaction.
   */
  private final Class<?> dataClass;

  /**
   * The delegate logic for handling the transaction.
   */
  private final Delegate<?, ?> delegate;

  <T extends TransactionData<R>, R> TransactionHandlerRegistry(final Class<T> dataClass,
                                                                final Delegate<T, R> delegate) {
    this.dataClass = dataClass;
    this.delegate = delegate;
  }

  /**
   * Creates a {@link TransactionHandler} bound to the given server instance.
   *
   * @param server the server to pass to the handler
   * @return a new transaction handler
   */
  @SuppressWarnings("unchecked")
  public <T extends TransactionData<R>, R> TransactionHandler<T, R> createTransactionHandler(
          final @NotNull VelocityServer server) {
    final Delegate<T, R> typedDelegate = (Delegate<T, R>) this.delegate;
    return new TransactionHandler<>((Class<T>) dataClass) {
      @Override
      public @Nullable CompletableFuture<R> handleData(final T data) {
        return typedDelegate.handleData(server, data);
      }
    };
  }

  /**
   * Functional interface for handling data in a transaction, used for
   * creating a response from the data.
   *
   * @param <T> the type of the data
   * @param <R> the type of the response data
   */
  @FunctionalInterface
  public interface Delegate<T extends TransactionData<R>, R> {

    /**
     * Handles the incoming data and produces a response asynchronously.
     *
     * @param server the {@link VelocityServer} handling the transaction
     * @param data   the incoming data
     * @return a future containing the response data, or {@code null} if no reply is required
     */
    @Nullable CompletableFuture<R> handleData(VelocityServer server, T data);
  }
}
