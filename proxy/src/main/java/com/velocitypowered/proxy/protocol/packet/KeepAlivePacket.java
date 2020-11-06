package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class KeepAlivePacket implements Packet {

  public static final Decoder<KeepAlivePacket> DECODER = (buf, direction, version) -> {
    final long randomId;
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_12_2) >= 0) {
      randomId = buf.readLong();
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      randomId = ProtocolUtils.readVarInt(buf);
    } else {
      randomId = buf.readInt();
    }
    return new KeepAlivePacket(randomId);
  };

  private final long randomId;

  public KeepAlivePacket(final long randomId) {
    this.randomId = randomId;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_12_2)) {
      buf.writeLong(randomId);
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeVarInt(buf, (int) randomId);
    } else {
      buf.writeInt((int) randomId);
    }
  }

  public long getRandomId() {
    return randomId;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "KeepAlive{"
      + "randomId=" + randomId
      + '}';
  }
}
