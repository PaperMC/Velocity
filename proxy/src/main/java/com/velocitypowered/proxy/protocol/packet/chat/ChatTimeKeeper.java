package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import java.time.Instant;

public class ChatTimeKeeper {
  private Instant lastTimestamp;

  public ChatTimeKeeper() {
    this.lastTimestamp = Instant.MIN;
  }

  public boolean update(Instant instant) {
    if (instant.isBefore(this.lastTimestamp)) {
      this.lastTimestamp = instant;
      return false;
    }
    this.lastTimestamp = instant;
    return true;
  }
}
