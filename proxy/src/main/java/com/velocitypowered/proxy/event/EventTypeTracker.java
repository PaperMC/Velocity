/*
 * Copyright (C) 2021 Velocity Contributors
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

package com.velocitypowered.proxy.event;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class EventTypeTracker {

  private final ConcurrentMap<Class<?>, ImmutableSet<Class<?>>> friends;

  public EventTypeTracker() {
    this.friends = new ConcurrentHashMap<>();
  }

  public Collection<Class<?>> getFriendsOf(final Class<?> eventType) {
    if (friends.containsKey(eventType)) {
      return friends.get(eventType);
    }

    final Collection<Class<?>> types = getEventTypes(eventType);
    for (Class<?> type : types) {
      if (type == eventType) {
        continue;
      }

      this.friends.merge(
          type,
          ImmutableSet.of(eventType),
          (oldVal, newSingleton) -> ImmutableSet.<Class<?>>builder()
              .addAll(oldVal)
              .addAll(newSingleton)
              .build()
      );
    }
    return types;
  }

  private static Collection<Class<?>> getEventTypes(final Class<?> eventType) {
    return TypeToken.of(eventType).getTypes().rawTypes().stream()
        .filter(type -> type != Object.class)
        .collect(Collectors.toList());
  }
}
