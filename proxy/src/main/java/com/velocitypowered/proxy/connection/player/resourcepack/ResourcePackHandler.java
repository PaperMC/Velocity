/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.connection.player.resourcepack;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.function.Predicate;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

/**
 * ResourcePackHandler.
 */
public abstract sealed class ResourcePackHandler
        permits LegacyResourcePackHandler, ModernResourcePackHandler {
  protected final Queue<ResourcePackInfo> outstandingResourcePacks = new ArrayDeque<>();
  protected @MonotonicNonNull Boolean previousResourceResponse;
  protected final ConnectedPlayer player;
  protected final VelocityServer server;

  protected ResourcePackHandler(ConnectedPlayer player, VelocityServer server) {
    this.player = player;
    this.server = server;
  }

  /**
   * Creates a new ResourcePackHandler.
   *
   * @param player the player.
   * @param server the velocity server
   *
   * @return a new ResourcePackHandler
   */
  public static ResourcePackHandler create(final ConnectedPlayer player,
                                           final VelocityServer server) {
    final ProtocolVersion protocolVersion = player.getProtocolVersion();
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_17)) {
      return new LegacyResourcePackHandler(player, server);
    }
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      return new Legacy117ResourcePackHandler(player, server);
    }
    return new ModernResourcePackHandler(player, server);
  }

  public abstract void sendResourcePackRequest(ResourcePackRequest resourcePacks);

  public abstract @Nullable ResourcePackInfo getFirstAppliedPack();

  public abstract @Nullable ResourcePackInfo getFirstPendingPack();

  public abstract Collection<ResourcePackInfo> getAppliedResourcePacks();

  public abstract Collection<ResourcePackInfo> getPendingResourcePacks();

  /**
   * Clears the applied resource pack field.
   */
  public abstract void clearAppliedResourcePacks();

  public abstract void removeIf(Predicate<ResourcePackInfo> removePredicate);

  /**
   * Queues a resource-pack for sending to the player and sends it immediately if the queue is
   * empty.
   */
  public void queueResourcePack(ResourcePackInfo info) {
    outstandingResourcePacks.add(info);
    if (outstandingResourcePacks.size() == 1) {
      tickResourcePackQueue();
    }
  }

  protected void tickResourcePackQueue() {
    ResourcePackInfo queued = outstandingResourcePacks.peek();

    if (queued != null) {
      // Check if the player declined a resource pack once already
      if (previousResourceResponse != null && !previousResourceResponse) {
        // If that happened we can flush the queue right away.
        // Unless its 1.17+ and forced it will come back denied anyway
        while (!outstandingResourcePacks.isEmpty()) {
          queued = outstandingResourcePacks.peek();
          if (queued.getShouldForce() && player.getProtocolVersion()
                  .noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
            break;
          }
          onResourcePackResponse(PlayerResourcePackStatusEvent.Status.DECLINED);
          queued = null;
        }
        if (queued == null) {
          // Exit as the queue was cleared
          return;
        }
      }

      final ResourcePackRequestPacket request = new ResourcePackRequestPacket();
      request.setId(queued.getId());
      request.setUrl(queued.getUrl());
      if (queued.getHash() != null) {
        request.setHash(ByteBufUtil.hexDump(queued.getHash()));
      } else {
        request.setHash("");
      }
      request.setRequired(queued.getShouldForce());
      request.setPrompt(queued.getPrompt() == null ? null :
              new ComponentHolder(player.getProtocolVersion(), queued.getPrompt()));

      player.getConnection().write(request);
    }
  }

  /**
   * Processes a client response to a sent resource-pack.
   */
  public abstract boolean onResourcePackResponse(PlayerResourcePackStatusEvent.Status status);
}
