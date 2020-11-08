package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
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
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, message);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "ServerboundChatPacket{"
      + "message='" + message + '\''
      + '}';
  }
}
