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

import com.velocityctd.proxy.queue.RedisVelocityQueueManager;
import com.velocityctd.proxy.queue.VelocityQueue;
import com.velocityctd.proxy.queue.VelocityQueueEntry;
import com.velocityctd.proxy.queue.redis.packet.VelocityQueueSync;
import com.velocityctd.proxy.queue.redis.packet.VelocityQueueTransfer;
import com.velocityctd.proxy.redis.data.VelocityActionBar;
import com.velocityctd.proxy.redis.data.VelocityAlert;
import com.velocityctd.proxy.redis.data.VelocityKick;
import com.velocityctd.proxy.redis.data.VelocityMessage;
import com.velocityctd.proxy.redis.data.VelocitySudo;
import com.velocityctd.proxy.redis.data.VelocitySwitchServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * Registry that holds all {@link RouteHandler} entries for one-way Redis messages.
 */
public enum RouteHandlerRegistry {

  /**
   * Handles the {@link VelocityAlert} data by sending a message to all players on the proxy.
   */
  VELOCITY_ALERT(VelocityAlert.class, (server, data) -> {
    if (data.component() != null) {
      server.sendMessage(data.component());
    }
  }),

  /**
   * Handles the {@link VelocitySwitchServer} data by switching the player to the specified server.
   */
  VELOCITY_SWITCH_SERVER(VelocitySwitchServer.class, (server, data) -> {
    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      return;
    }

    server.getServer(data.serverName()).ifPresent(targetServer ->
            player.createConnectionRequest(targetServer).connectWithIndication());
  }),

  /**
   * Handles the {@link VelocityMessage} data by sending a message to the specified target.
   */
  VELOCITY_MESSAGE(VelocityMessage.class, (server, data) -> {
    final Component component = data.component();
    if (component == null) {
      return;
    }

    server.getPlayer(data.uniqueId()).ifPresent(player -> player.sendMessage(component));
  }),

  /**
   * Handles the {@link VelocityActionBar} data by sending an action bar to the specified target.
   */
  VELOCITY_ACTION_BAR(VelocityActionBar.class, (server, data) -> {
    if (data.component() == null) {
      return;
    }

    server.getPlayer(data.uniqueId()).ifPresent(player -> player.sendActionBar(data.component()));
  }),

  /**
   * Handles the {@link VelocitySudo} data by letting the specified player execute a command or chat message.
   */
  VELOCITY_SUDO(VelocitySudo.class, (server, data) -> {
    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      return;
    }

    final String message = data.message();
    if (message.startsWith("/")) {
      final String fullCommand = message.substring(1);
      final String commandLabel = fullCommand.split(" ")[0];
      if (server.getCommandManager().hasCommand(commandLabel)) {
        server.getCommandManager().executeAsync(player, fullCommand);
        return;
      }
    }

    player.spoofChatInput(message);
  }),

  /**
   * Handles the {@link VelocityKick} data by kicking the specified player with a reason.
   */
  VELOCITY_KICK(VelocityKick.class, (server, data) -> {
    final String targetProxyId = data.targetProxyId();
    if (targetProxyId != null && !targetProxyId.equalsIgnoreCase(server.getRedis().getProxyId())) {
      return;
    }

    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      return;
    }

    Component component = data.component();
    if (component == null) {
      component = Component.text("You have been kicked from the proxy.", NamedTextColor.RED);
    }

    player.disconnect0(component, true);
  }),

  /**
   * Handles the {@link VelocityQueueSync} data by applying the state change to the local queue.
   */
  VELOCITY_QUEUE_SYNC(VelocityQueueSync.class, (server, data) -> {
    RedisVelocityQueueManager redisVelocityQueueManager = ((RedisVelocityQueueManager) server.getQueueManager());
    redisVelocityQueueManager.handleSync(data);
  }),

  /**
   * Handles the {@link VelocityQueueTransfer} data by transferring the player to their target server.
   */
  VELOCITY_QUEUE(VelocityQueueTransfer.class, (server, data) -> {
    final VelocityQueue queue;
    try {
      queue = server.getQueueManager().getQueue(data.queueName());
    } catch (IllegalArgumentException ignored) {
      return; // unknown server - stale or malformed packet
    }

    final ConnectedPlayer player = server.getPlayer(data.uniqueId()).orElse(null);
    if (player == null) {
      if (!server.getRedis().getPlayerService().isPlayerOnline(data.uniqueId())) {
        final VelocityQueueEntry entry = queue.getEntry(data.uniqueId());
        if (entry != null) {
          entry.abortTransfer();
        }
      }
      return;
    }

    final VelocityQueueEntry entry = queue.getEntry(data.uniqueId());
    if (entry == null) {
      return;
    }

    entry.handleTransfer();
  });

  /**
   * The data class handled by this route.
   */
  private final Class<?> dataClass;

  /**
   * The route handler logic accepting the server and data.
   */
  private final BiConsumer<VelocityServer, ?> route;

  <T> RouteHandlerRegistry(final Class<T> dataClass, final @NotNull BiConsumer<VelocityServer, T> route) {
    this.dataClass = dataClass;
    this.route = route;
  }

  /**
   * Creates a {@link RouteHandler} bound to the given server instance.
   *
   * @param server the server to pass to the handler
   * @return a new route handler
   */
  @SuppressWarnings("unchecked")
  public <T> RouteHandler<T> createRouteHandler(final @NotNull VelocityServer server) {
    final BiConsumer<VelocityServer, T> typedRoute = (BiConsumer<VelocityServer, T>) this.route;
    return RouteHandler.consumer((Class<T>) dataClass, data -> typedRoute.accept(server, data));
  }
}
