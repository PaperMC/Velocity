package com.velocitypowered.proxy.messages;

public final class ForwardingType {

  private ForwardingType() { }

  public static final byte SERVER = 0x00;
  public static byte PLAYER = 0x01;
  public static final byte BROADCAST = 0x02;

  /**
   * Returns {@code true} and only true if specified type of
   * forwarding type exists.
   */
  public static boolean isValid(byte type) {
    return type >= SERVER && type <= BROADCAST;
  }
}
