package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class ServerboundServerLoginPacket implements Packet {
  private static final QuietDecoderException EMPTY_USERNAME = new QuietDecoderException("Empty username!");

  public static final PacketReader<ServerboundServerLoginPacket> DECODER = (buf, version) -> {
    final String username = ProtocolUtils.readString(buf, 16);
    if (username.isEmpty()) {
      throw EMPTY_USERNAME;
    }
    return new ServerboundServerLoginPacket(username);
  };
  public static final PacketWriter<ServerboundServerLoginPacket> ENCODER = (buf, packet, version) ->
    ProtocolUtils.writeString(buf, packet.username);

  private final String username;

  public ServerboundServerLoginPacket(String username) {
    this.username = Objects.requireNonNull(username, "username");
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("username", this.username)
      .toString();
  }
}
