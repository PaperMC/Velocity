package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundChatPacket implements Packet {

  public static final Decoder<ServerboundChatPacket> DECODER = (buf, direction, version) -> {
    final String message = ProtocolUtils.readString(buf);
    return new ServerboundChatPacket(message);
  };

  public static final int MAX_MESSAGE_LENGTH = 256;

  private final @Nullable String message;

  public ServerboundChatPacket(String message) {
    this.message = message;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    ProtocolUtils.writeString(buf, message);
  }

  public String getMessage() {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    return message;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "Chat{"
      + "message='" + message + '\''
      + '}';
  }
}
