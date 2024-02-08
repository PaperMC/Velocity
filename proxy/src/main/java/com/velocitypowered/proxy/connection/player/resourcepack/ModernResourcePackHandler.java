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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modern (Minecraft 1.20.3+) ResourcePackHandler
 */
public final class ModernResourcePackHandler extends ResourcePackHandler {
  private final ListMultimap<UUID, ResourcePackInfo> outstandingResourcePacks =
      Multimaps.newListMultimap(new ConcurrentHashMap<>(), LinkedList::new);
  private final Map<UUID, ResourcePackInfo> pendingResourcePacks = new ConcurrentHashMap<>();
  private final Map<UUID, ResourcePackInfo> appliedResourcePacks = new ConcurrentHashMap<>();

  ModernResourcePackHandler(final ConnectedPlayer player, final VelocityServer server) {
    super(player, server);
  }

  @Override
  public @Nullable ResourcePackInfo getFirstAppliedPack() {
    if (appliedResourcePacks.isEmpty()) {
      return null;
    }
    return appliedResourcePacks.values().iterator().next();
  }

  @Override
  public @Nullable ResourcePackInfo getFirstPendingPack() {
    if (pendingResourcePacks.isEmpty()) {
      return null;
    }
    return pendingResourcePacks.values().iterator().next();
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getAppliedResourcePacks() {
    return List.copyOf(appliedResourcePacks.values());
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getPendingResourcePacks() {
    return List.copyOf(pendingResourcePacks.values());
  }

  @Override
  public void clearAppliedResourcePacks() {
    this.outstandingResourcePacks.clear();
    this.pendingResourcePacks.clear();
    this.appliedResourcePacks.clear();
  }

  @Override
  public boolean remove(final @NotNull UUID uuid) {
    outstandingResourcePacks.removeAll(uuid);
    return appliedResourcePacks.remove(uuid) != null | pendingResourcePacks.remove(uuid) != null;
  }

  @Override
  public void queueResourcePack(final @NotNull ResourcePackInfo info) {
    final List<ResourcePackInfo> outstandingResourcePacks =
        this.outstandingResourcePacks.get(info.getId());
    outstandingResourcePacks.add(info);
    if (outstandingResourcePacks.size() == 1) {
      tickResourcePackQueue(outstandingResourcePacks.get(0).getId());
    }
  }

  @Override
  public void queueResourcePack(final @NotNull ResourcePackRequest request) {
    if (request.packs().size() > 1) {
      player.getBundleHandler().bundlePackets(() -> super.queueResourcePack(request));
    } else {
      super.queueResourcePack(request);
    }
  }

  private void tickResourcePackQueue(final @NotNull UUID uuid) {
    final List<ResourcePackInfo> outstandingResourcePacks =
        this.outstandingResourcePacks.get(uuid);
    if (!outstandingResourcePacks.isEmpty()) {
      sendResourcePackRequestPacket(outstandingResourcePacks.get(0));
    }
  }

  @Override
  public boolean onResourcePackResponse(
          final @NotNull ResourcePackResponseBundle bundle
  ) {
    final UUID uuid = bundle.uuid();
    final List<ResourcePackInfo> outstandingResourcePacks =
        this.outstandingResourcePacks.get(uuid);
    final boolean peek = bundle.status().isIntermediate();
    final ResourcePackInfo queued = outstandingResourcePacks.isEmpty() ? null :
        peek ? outstandingResourcePacks.get(0) : outstandingResourcePacks.remove(0);

    server.getEventManager()
            .fire(new PlayerResourcePackStatusEvent(this.player, uuid, bundle.status(), queued))
            .thenAcceptAsync(event -> {
              if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
                      && event.getPackInfo() != null && event.getPackInfo().getShouldForce()
                      && !event.isOverwriteKick()
              ) {
                player.disconnect(Component
                        .translatable("multiplayer.requiredTexturePrompt.disconnect"));
              }
            });

    switch (bundle.status()) {
      // The player has accepted the resource pack and will proceed to download it.
      case ACCEPTED -> {
        if (queued != null) {
          pendingResourcePacks.put(uuid, queued);
        }
      }
      // The resource pack has been applied correctly.
      case SUCCESSFUL -> {
        if (queued != null) {
          appliedResourcePacks.put(uuid, queued);
        }
        pendingResourcePacks.remove(uuid);
      }
      // An error occurred while trying to download the resource pack to the client,
      // so the resource pack cannot be applied.
      case DISCARDED, DECLINED, FAILED_RELOAD, FAILED_DOWNLOAD, INVALID_URL -> {
        pendingResourcePacks.remove(uuid);
        appliedResourcePacks.remove(uuid);
      }
      // The other cases in which no action is taken are documented in the javadocs.
      default -> {
      }
    }

    if (!peek) {
      player.getConnection().eventLoop().execute(() -> tickResourcePackQueue(uuid));
    }

    return handleResponseResult(queued, bundle);
  }

  @Override
  public boolean hasPackAppliedByHash(final byte[] hash) {
    if (hash == null) {
      return false;
    }
    for (final Map.Entry<UUID, ResourcePackInfo> appliedPack : appliedResourcePacks.entrySet()) {
      if (Arrays.equals(appliedPack.getValue().getHash(), hash)) {
        return true;
      }
    }
    return false;
  }
}
