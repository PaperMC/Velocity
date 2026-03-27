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

package com.velocityctd.proxy.connection.profile.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocityctd.proxy.connection.profile.GameProfileKey;
import com.velocitypowered.api.util.GameProfile;
import java.time.Duration;
import java.util.Optional;

public class MemoryGameProfileCache implements GameProfileCacheStrategy {

  private final Cache<GameProfileKey, GameProfile> cache;

  public MemoryGameProfileCache(Duration cacheExpiry, int maximumSize) {
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(cacheExpiry)
        .maximumSize(maximumSize)
        .build();
  }

  @Override
  public Optional<GameProfile> findByUsername(String username, String playerIp) {
    return Optional.ofNullable(cache.getIfPresent(new GameProfileKey(username, playerIp)));
  }

  @Override
  public void insert(GameProfile profile, String playerIp) {
    cache.put(new GameProfileKey(profile.getName(), playerIp), profile);
  }
}
