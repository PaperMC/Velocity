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

package com.velocityctd.proxy.cluster.redis;

import com.velocityctd.api.queue.QueueEntryData;
import com.velocityctd.proxy.cluster.VelocityClusterPlayer;
import com.velocityctd.proxy.redis.VelocityRedis;
import com.velocityctd.proxy.redis.data.VelocityGetPlayerPing;
import com.velocityctd.proxy.redis.data.VelocityKick;
import com.velocityctd.proxy.redis.data.VelocityMessage;
import com.velocityctd.proxy.redis.data.VelocitySudo;
import com.velocityctd.proxy.redis.data.VelocitySwitchServer;
import com.velocityctd.proxy.redis.data.VelocityTransferRemote;
import com.velocityctd.proxy.redis.depot.player.PlayerEntry;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Redis-backed implementation of {@link VelocityClusterPlayer}.
 */
public final class RedisClusterPlayer implements VelocityClusterPlayer {

  private final VelocityServer server;
  private final VelocityRedis redis;
  private final PlayerEntry redisEntry;

  RedisClusterPlayer(final VelocityServer server, final PlayerEntry redisEntry) {
    this.server = server;
    this.redis = server.getRedis();
    this.redisEntry = redisEntry;
  }

  @Override
  public UUID getUniqueId() {
    return redisEntry.getUniqueId();
  }

  @Override
  public String getUsername() {
    return redisEntry.getUsername();
  }

  @Override
  public String getProxyId() {
    return redisEntry.getProxyId();
  }

  @Override
  public @Nullable String getServerName() {
    return redisEntry.getServerName();
  }

  @Override
  public @Nullable String getIpAddress() {
    return redisEntry.getIpAddress();
  }

  @Override
  public boolean isClientListingAllowed() {
    return redisEntry.isClientListingAllowed();
  }

  @Override
  public void kick(final Component reason) {
    redis.publish(new VelocityKick(redisEntry.getUniqueId(), reason));
  }

  @Override
  public void sudo(final String command) {
    redis.publish(new VelocitySudo(redisEntry.getUniqueId(), command));
  }

  @Override
  public void move(final String targetServer) {
    redis.publish(new VelocitySwitchServer(redisEntry.getUniqueId(), targetServer));
  }

  @Override
  public CompletableFuture<Boolean> transfer(final String ip, final int port) {
    return redis.publishTransaction(new VelocityTransferRemote(redisEntry.getUniqueId(), ip, port));
  }

  @Override
  public void sendMessage(final Component message) {
    redis.publish(new VelocityMessage(redisEntry.getUniqueId(), message));
  }

  @Override
  public CompletableFuture<Long> queryPing() {
    return redis.publishTransaction(new VelocityGetPlayerPing(redisEntry.getUniqueId()));
  }

  @Override
  public QueueEntryData toQueueEntryData(final String serverName) {
    return new QueueEntryData(
        redisEntry.getUniqueId(),
        redisEntry.getUsername(),
        redisEntry.getQueuePriorities().getOrDefault(serverName, 0),
        redisEntry.isFullServerBypass(),
        redisEntry.isQueueBypass()
    );
  }

  @Override
  public Optional<ConnectedPlayer> toLocalPlayer() {
    return server.getPlayer(redisEntry.getUniqueId());
  }
}
