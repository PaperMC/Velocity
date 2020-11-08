package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent.Status;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundResourcePackResponsePacket implements Packet {
  public static final Decoder<ServerboundResourcePackResponsePacket> DECODER = (buf, direction, version) -> {
    final String hash;
    if (version.lte(ProtocolVersion.MINECRAFT_1_9_4)) {
      hash = ProtocolUtils.readString(buf);
    } else {
      hash = null;
    }
    final Status status = Status.values()[ProtocolUtils.readVarInt(buf)];
    return new ServerboundResourcePackResponsePacket(hash, status);
  };

  private final @Nullable String hash;
  private final Status status;

  public ServerboundResourcePackResponsePacket(final @Nullable String hash, final Status status) {
    this.hash = hash;
    this.status = status;
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.lte(ProtocolVersion.MINECRAFT_1_9_4)) {
      ProtocolUtils.writeString(buf, hash);
    }
    ProtocolUtils.writeVarInt(buf, status.ordinal());
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public Status getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("hash", this.hash)
      .add("status", this.status)
      .toString();
  }
}
