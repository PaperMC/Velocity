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

package com.velocityctd.proxy.redis.profilecache;

import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;

import com.velocityctd.proxy.connection.profile.cache.GameProfileCacheStrategy;
import com.velocityctd.proxy.redis.provider.RedisProvider;
import com.velocitypowered.api.util.GameProfile;
import java.time.Duration;
import java.util.Optional;

public class RedisGameProfileCache implements GameProfileCacheStrategy {

  private static final String KEY_PREFIX = "velocity:profile-cache:";

  private final RedisProvider provider;
  private final long ttlSeconds;

  public RedisGameProfileCache(RedisProvider provider, Duration cacheExpiry) {
    this.provider = provider;
    this.ttlSeconds = cacheExpiry.toSeconds();
  }

  @Override
  public Optional<GameProfile> findByUsername(String username, String playerIp) {
    return Optional.ofNullable(provider.get(KEY_PREFIX + username + ":" + playerIp))
        .map(json -> GENERAL_GSON.fromJson(json, GameProfile.class));
  }

  @Override
  public void insert(GameProfile profile, String playerIp) {
    String json = GENERAL_GSON.toJson(profile);
    provider.setWithExpiry(KEY_PREFIX + profile.getName() + ":" + playerIp, json, ttlSeconds);
  }
}
