/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.util.GameProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A global cache for player profiles to reduce Mojang API hits.
 */
public final class GlobalProfileCache {
  private final Cache<UUID, GameProfile> profileCache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.HOURS)
      .maximumSize(10000)
      .build();

  private final Cache<String, UUID> nameToUuidCache = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.HOURS)
      .maximumSize(10000)
      .build();

  /**
   * Caches a player profile.
   *
   * @param profile the profile to cache
   */
  public void cacheProfile(GameProfile profile) {
    profileCache.put(profile.getId(), profile);
    nameToUuidCache.put(profile.getName().toLowerCase(), profile.getId());
  }

  /**
   * Gets a cached player profile by UUID.
   *
   * @param uuid the UUID of the player
   * @return the cached profile, if present
   */
  public Optional<GameProfile> getProfile(UUID uuid) {
    return Optional.ofNullable(profileCache.getIfPresent(uuid));
  }

  /**
   * Gets a cached player UUID by name.
   *
   * @param name the name of the player
   * @return the cached UUID, if present
   */
  public Optional<UUID> getUuid(String name) {
    return Optional.ofNullable(nameToUuidCache.getIfPresent(name.toLowerCase()));
  }
}
