/*
 * Copyright (C) 2023 Velocity Contributors
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

package com.velocitypowered.proxy.adventure;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * Click callback manager.
 */
public class ClickCallbackManager {
  public static final ClickCallbackManager INSTANCE = new ClickCallbackManager();

  static final String COMMAND = "/velocity:callback ";

  private final Cache<UUID, RegisteredCallback> registrations = Caffeine.newBuilder()
      .expireAfter(new Expiry<UUID, RegisteredCallback>() {
        @Override
        public long expireAfterCreate(UUID key, RegisteredCallback value, long currentTime) {
          return value.duration().toNanos();
        }

        @Override
        public long expireAfterUpdate(UUID key, RegisteredCallback value, long currentTime,
                                      @NonNegative long currentDuration) {
          return currentDuration;
        }

        @Override
        public long expireAfterRead(UUID key, RegisteredCallback value, long currentTime,
                                    @NonNegative long currentDuration) {
          final AtomicInteger remainingUses = value.remainingUses();
          if (remainingUses != null && remainingUses.get() <= 0) {
            return 0;
          }
          return currentDuration;
        }
      })
      .scheduler(Scheduler.systemScheduler())
      .build();

  private ClickCallbackManager() {
  }

  /**
   * Run a callback.
   *
   * @param audience the audience
   * @param id       the callback's ID
   * @return {@code true} if the callback was run, {@code false} if not
   */
  public boolean runCallback(final Audience audience, final UUID id) {
    final RegisteredCallback callback = this.registrations.getIfPresent(id);
    if (callback != null && callback.tryUse()) {
      callback.callback().accept(audience);
      return true;
    }
    return false;
  }

  /**
   * Registers a click callback.
   *
   * @param callback the callback to register
   * @param options  associated options
   * @return the callback ID
   */
  public UUID register(
      final ClickCallback<Audience> callback,
      final ClickCallback.Options options
  ) {
    final UUID id = UUID.randomUUID();
    final RegisteredCallback registration = new RegisteredCallback(
        options.lifetime(),
        options.uses(),
        callback
    );
    this.registrations.put(id, registration);
    return id;
  }
}
