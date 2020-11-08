package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;

public class ServerboundChatPacket implements Packet {
  public static final Decoder<ServerboundChatPacket> DECODER = (buf, direction, version) -> {
    final String message = ProtocolUtils.readString(buf);
    return new ServerboundChatPacket(message);
  };

  public static final int MAX_MESSAGE_LENGTH = 256;

  private final String message;

  public ServerboundChatPacket(String message) {
    this.message = message;
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, message);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("message", this.message)
      .toString();
  }
}
