package com.velocitypowered.proxy.network.packet;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.function.LongFunction;

public abstract class AbstractKeepAlivePacket implements Packet {
  protected static <P extends AbstractKeepAlivePacket> Decoder<P> decoder(final LongFunction<P> factory) {
    return (buf, direction, version) -> {
      final long randomId;
      if (version.gte(ProtocolVersion.MINECRAFT_1_12_2)) {
        randomId = buf.readLong();
      } else if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
        randomId = ProtocolUtils.readVarInt(buf);
      } else {
        randomId = buf.readInt();
      }
      return factory.apply(randomId);
    };
  }

  private final long randomId;

  protected AbstractKeepAlivePacket(final long randomId) {
    this.randomId = randomId;
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("randomId", this.randomId)
      .toString();
  }
}
