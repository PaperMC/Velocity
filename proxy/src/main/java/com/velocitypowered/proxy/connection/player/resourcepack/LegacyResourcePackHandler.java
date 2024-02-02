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
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy (Minecraft <1.17) ResourcePackHandler.
 */
public sealed class LegacyResourcePackHandler extends ResourcePackHandler
        permits Legacy117ResourcePackHandler {
  private @Nullable ResourcePackInfo pendingResourcePack;
  private @Nullable ResourcePackInfo appliedResourcePack;

  LegacyResourcePackHandler(ConnectedPlayer player, VelocityServer server) {
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
    this.appliedResourcePack = null;
  }

  @Override
  public void removeIf(final @NotNull Predicate<ResourcePackInfo> removePredicate) {
    if (appliedResourcePack != null && removePredicate.test(appliedResourcePack)) {
      appliedResourcePack = null;
    }
  }

  @Override
  public boolean onResourcePackResponse(
          final PlayerResourcePackStatusEvent.@NotNull Status status
  ) {
    final boolean peek = status.isIntermediate();
    final ResourcePackInfo queued = peek
            ? outstandingResourcePacks.peek() : outstandingResourcePacks.poll();

    server.getEventManager().fire(new PlayerResourcePackStatusEvent(this.player, status, queued))
            .thenAcceptAsync(event -> {
              if (shouldDisconnectForForcePack(event)) {
                event.getPlayer().disconnect(Component
                        .translatable("multiplayer.requiredTexturePrompt.disconnect"));
              }
            });

    switch (status) {
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

    return queued != null
            && queued.getOriginalOrigin() != ResourcePackInfo.Origin.DOWNSTREAM_SERVER;
  }

  protected boolean shouldDisconnectForForcePack(final PlayerResourcePackStatusEvent event) {
    return event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
            && event.getPackInfo() != null && event.getPackInfo().getShouldForce();
  }
}
