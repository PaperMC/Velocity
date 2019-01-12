package com.velocitypowered.proxy.messages;

public final class VelocityReplyCodes {

  private VelocityReplyCodes() { }

  private static byte nextCode;

  // Initializes as follows
  public static final byte SUCCESS = nextCode();
  public static final byte API_MISMATCH = nextCode();
  public static final byte INVALID_ACTION = nextCode();
  public static final byte UNKNOWN_FORWARDING_TYPE = nextCode();
  public static final byte UNKNOWN_FORWARDING_SINK = nextCode();
  public static final byte UNKNOWN_PLAYER_REPRESENTATION = nextCode();
  public static final byte UNKNOWN_PLAYER = nextCode();
  public static final byte UNKNOWN_SERVER = nextCode();
  public static final byte UNEXPECTED_ERROR = nextCode();

  private static byte nextCode() {
    return nextCode++;
  }
}
