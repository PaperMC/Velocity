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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Modern (Minecraft 1.20.3+) ResourcePackHandler
 */
public final class ModernResourcePackHandler extends ResourcePackHandler {
  private final List<ResourcePackInfo> pendingResourcePacks = new ArrayList<>();
  private final List<ResourcePackInfo> appliedResourcePacks = new ArrayList<>();

  ModernResourcePackHandler(ConnectedPlayer player, VelocityServer server) {
    super(player, server);
  }

  @Override
  public void sendResourcePackRequest(ResourcePackRequest resourcePacks) {

  }

  @Override
  public @Nullable ResourcePackInfo getFirstAppliedPack() {
    if (appliedResourcePacks.isEmpty()) {
      return null;
    }
    return appliedResourcePacks.get(0);
  }

  @Override
  public @Nullable ResourcePackInfo getFirstPendingPack() {
    if (pendingResourcePacks.isEmpty()) {
      return null;
    }
    return pendingResourcePacks.get(0);
  }

  @Override
  public Collection<ResourcePackInfo> getAppliedResourcePacks() {
    return List.copyOf(appliedResourcePacks);
  }

  @Override
  public Collection<ResourcePackInfo> getPendingResourcePacks() {
    return List.copyOf(pendingResourcePacks);
  }

  @Override
  public void clearAppliedResourcePacks() {
    this.appliedResourcePacks.clear();
  }

  @Override
  public void removeIf(Predicate<ResourcePackInfo> removePredicate) {
    appliedResourcePacks.removeIf(removePredicate);
  }

  @Override
  public boolean onResourcePackResponse(PlayerResourcePackStatusEvent.Status status) {
    final boolean peek = status.isIntermediate();
    final ResourcePackInfo queued = peek
            ? outstandingResourcePacks.peek() : outstandingResourcePacks.poll();

    server.getEventManager().fire(new PlayerResourcePackStatusEvent(this.player, status, queued))
            .thenAcceptAsync(event -> {
              if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
                      && event.getPackInfo() != null && event.getPackInfo().getShouldForce()
                      && !event.isOverwriteKick()
              ) {
                player.disconnect(Component
                        .translatable("multiplayer.requiredTexturePrompt.disconnect"));
              }
            });

    switch (status) {
      case ACCEPTED -> {
        previousResourceResponse = true;
        pendingResourcePacks.add(queued);
      }
      case DECLINED -> previousResourceResponse = false;
      case SUCCESSFUL -> {
        appliedResourcePacks.add(queued);
        if (queued != null) {
          pendingResourcePacks.removeIf(resourcePackInfo -> {
            if (resourcePackInfo.getId() == null) {
              return resourcePackInfo.getUrl().equals(queued.getUrl())
                      && Arrays.equals(resourcePackInfo.getHash(), queued.getHash());
            }
            return resourcePackInfo.getId().equals(queued.getId());
          });
        }
      }
      case FAILED_DOWNLOAD -> {
        if (queued != null) {
          pendingResourcePacks.removeIf(resourcePackInfo -> {
            if (resourcePackInfo.getId() == null) {
              return resourcePackInfo.getUrl().equals(queued.getUrl())
                      && Arrays.equals(resourcePackInfo.getHash(), queued.getHash());
            }
            return resourcePackInfo.getId().equals(queued.getId());
          });
        }
      }
      case DISCARDED -> {
        if (queued != null && queued.getId() != null) {
          appliedResourcePacks.removeIf(
                  resourcePackInfo -> queued.getId().equals(resourcePackInfo.getId()));
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
}
