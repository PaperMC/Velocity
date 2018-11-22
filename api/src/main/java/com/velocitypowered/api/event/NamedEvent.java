package com.velocitypowered.api.event;

/**
 * This event is a proxy, for all events that have only one toString method, 
 * it only returns the name of its class.
 *
 * @author Lars Artmann | LartyHD
 */
public class NamedEvent {

  protected NamedEvent() {
    throw new AssertionError("Don't create a simple instance of NamedEvent");
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
  
}
