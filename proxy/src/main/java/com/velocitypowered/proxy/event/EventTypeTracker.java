/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.TypeToken;
import java.util.List;
import java.util.stream.Collectors;

class EventTypeTracker {

  private final ListMultimap<Class<?>, Class<?>> friends;

  public EventTypeTracker() {
    this.friends = ArrayListMultimap.create();
  }

  public List<Class<?>> getFriendsOf(final Class<?> eventType) {
    if (friends.containsKey(eventType)) {
      return ImmutableList.copyOf(friends.get(eventType));
    }

    final List<Class<?>> types = getEventTypes(eventType);
    for (Class<?> type : types) {
      if (type == eventType) {
        continue;
      }

      friends.put(type, eventType);
    }
    return types;
  }

  private static List<Class<?>> getEventTypes(final Class<?> eventType) {
    return TypeToken.of(eventType).getTypes().rawTypes().stream()
        .filter(type -> type != Object.class)
        .collect(Collectors.toList());
  }
}
