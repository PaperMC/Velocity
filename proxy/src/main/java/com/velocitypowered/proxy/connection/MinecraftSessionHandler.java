package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler extends PacketHandler {

  default boolean beforeHandle() {
    return false;
  }

  default void handleGeneric(Packet packet) {
  }

  default void handleUnknown(ByteBuf buf) {
  }

  default void connected() {
  }

  default void disconnected() {
  }

  default void activated() {
  }

  default void deactivated() {
  }

  default void exception(Throwable throwable) {
  }

  default void writabilityChanged() {
  }

  default void readCompleted() {
  }
}
