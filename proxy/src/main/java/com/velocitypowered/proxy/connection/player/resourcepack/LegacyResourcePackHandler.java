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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy (Minecraft &lt;1.17) ResourcePackHandler.
 */
public sealed class LegacyResourcePackHandler extends ResourcePackHandler
        permits Legacy117ResourcePackHandler {
  protected @MonotonicNonNull Boolean previousResourceResponse;
  protected final Queue<ResourcePackInfo> outstandingResourcePacks = new ArrayDeque<>();
  private @Nullable ResourcePackInfo pendingResourcePack;
  private @Nullable ResourcePackInfo appliedResourcePack;

  LegacyResourcePackHandler(final ConnectedPlayer player, final VelocityServer server) {
    super(player, server);
  }

  @Override
  @Nullable
  public ResourcePackInfo getFirstAppliedPack() {
    return appliedResourcePack;
  }

  @Override
  @Nullable
  public ResourcePackInfo getFirstPendingPack() {
    return pendingResourcePack;
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getAppliedResourcePacks() {
    if (appliedResourcePack == null) {
      return List.of();
    }
    return List.of(appliedResourcePack);
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getPendingResourcePacks() {
    if (pendingResourcePack == null) {
      return List.of();
    }
    return List.of(pendingResourcePack);
  }

  @Override
  public void clearAppliedResourcePacks() {
    // This is valid only for players with 1.20.2 versions
    this.appliedResourcePack = null;
  }

  @Override
  public boolean remove(final @NotNull UUID id) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot remove a ResourcePack from a legacy client");
  }

  @Override
  public void queueResourcePack(@NotNull ResourcePackInfo info) {
    outstandingResourcePacks.add(info);
    if (outstandingResourcePacks.size() == 1) {
      tickResourcePackQueue();
    }
  }

  private void tickResourcePackQueue() {
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
          onResourcePackResponse(new ResourcePackResponseBundle(queued.getId(),
                  queued.getHash() == null ? "" : new String(queued.getHash()),
                  PlayerResourcePackStatusEvent.Status.DECLINED));
          queued = null;
        }
        if (queued == null) {
          // Exit as the queue was cleared
          return;
        }
      }

      sendResourcePackRequestPacket(queued);
    }
  }

  @Override
  public boolean onResourcePackResponse(
          final @NotNull ResourcePackResponseBundle bundle
  ) {
    final boolean peek = bundle.status().isIntermediate();
    final ResourcePackInfo queued = peek
            ? outstandingResourcePacks.peek() : outstandingResourcePacks.poll();

    server.getEventManager()
            .fire(new PlayerResourcePackStatusEvent(
                this.player, bundle.uuid(), bundle.status(), queued))
            .thenAcceptAsync(event -> {
              if (shouldDisconnectForForcePack(event)) {
                event.getPlayer().disconnect(Component
                        .translatable("multiplayer.requiredTexturePrompt.disconnect"));
              }
            });

    switch (bundle.status()) {
      case ACCEPTED -> {
        previousResourceResponse = true;
        pendingResourcePack = queued;
      }
      case DECLINED -> previousResourceResponse = false;
      case SUCCESSFUL -> {
        appliedResourcePack = queued;
        pendingResourcePack = null;
      }
      case FAILED_DOWNLOAD -> pendingResourcePack = null;
      case DISCARDED -> {
        if (queued != null && queued.getId() != null
                && appliedResourcePack != null
                && appliedResourcePack.getId().equals(queued.getId())) {
          appliedResourcePack = null;
        }
      }
      default -> {
      }
    }

    if (!peek) {
      player.getConnection().eventLoop().execute(this::tickResourcePackQueue);
    }

    return handleResponseResult(queued, bundle);
  }

  @Override
  public boolean hasPackAppliedByHash(final byte[] hash) {
    if (hash == null) {
      return false;
    }

    return this.appliedResourcePack != null
            && Arrays.equals(this.appliedResourcePack.getHash(), hash);
  }

  protected boolean shouldDisconnectForForcePack(final PlayerResourcePackStatusEvent event) {
    return event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
            && event.getPackInfo() != null && event.getPackInfo().getShouldForce();
  }
}
