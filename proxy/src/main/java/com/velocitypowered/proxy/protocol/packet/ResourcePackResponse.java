package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ResourcePackResponse implements MinecraftPacket {

  @MonotonicNonNull
  private Status status;

  public Status getStatus() {
    if (status == null) {
      throw new IllegalStateException("Packet not yet deserialized");
    }
    return status;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.status = Status.values()[ProtocolUtils.readVarInt(buf)];
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, status.ordinal());
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "ResourcePackResponse{"
        + "status=" + status
        + '}';
  }
}
