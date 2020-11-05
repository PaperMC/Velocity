package com.velocitypowered.proxy.event;

import com.velocitypowered.api.event.EventTask;

public interface UntargetedEventHandler {

  EventTask execute(Object targetInstance, Object event);

  interface Void extends UntargetedEventHandler {

    @Override
    default EventTask execute(final Object targetInstance, final Object event) {
      executeVoid(targetInstance, event);
      return null;
    }

    void executeVoid(Object targetInstance, Object event);
  }
}
