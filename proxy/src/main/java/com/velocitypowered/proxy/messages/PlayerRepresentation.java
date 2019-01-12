package com.velocitypowered.proxy.messages;

public final class PlayerRepresentation {

  private PlayerRepresentation() { }

  public static final byte UUID = 0x00;
  public static final byte NAME = 0x01;

  /**
   * Returns {@code true} and only true if specified type of
   * representation exists.
   */
  public static boolean isValid(byte type) {
    return UUID == type || NAME == type;
  }
}
