package com.velocitypowered.api.event;

/**
 * This event is a proxy, for all events that have only one toString method, 
 * it only returns the name of its class.
 *
 * @author Lars Artmann | LartyHD
 */
public final class NamedEvent {

  protected NamedEvent() {
    throw new AssertionError();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
  
}
