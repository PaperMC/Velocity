package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ServerboundChatPacket implements Packet {
  public static final PacketReader<ServerboundChatPacket> DECODER = (buf, version) -> {
    final String message = ProtocolUtils.readString(buf);
    return new ServerboundChatPacket(message);
  };
  public static final PacketWriter<ServerboundChatPacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeString(buf, packet.message);

  public static final int MAX_MESSAGE_LENGTH = 256;

  private final String message;

  public ServerboundChatPacket(String message) {
    this.message = message;
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
