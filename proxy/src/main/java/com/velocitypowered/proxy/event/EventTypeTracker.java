package com.velocitypowered.proxy.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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
