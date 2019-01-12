package com.velocitypowered.proxy.messages;

public final class VelocityActions {

  private VelocityActions() { }

  public static final byte IDENTIFY = 0x00;
  public static final byte FETCH_SERVERS = 0x01;
  public static final byte CONNECT = 0x02;
  public static final byte FORWARD = 0x03;
  public static final byte LOCATE = 0x05;
  public static final byte SERVER_PLAYERS = 0x06;
}
