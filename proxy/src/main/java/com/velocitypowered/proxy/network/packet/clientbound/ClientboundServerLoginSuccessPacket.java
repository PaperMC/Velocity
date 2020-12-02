package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;

public class ClientboundServerLoginSuccessPacket implements Packet {
  public static final PacketReader<ClientboundServerLoginSuccessPacket> DECODER = (buf, direction, version) -> {
    final UUID uuid;
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      uuid = ProtocolUtils.readUuidIntArray(buf);
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_7_6)) {
      uuid = UUID.fromString(ProtocolUtils.readString(buf, 36));
    } else {
      uuid = UuidUtils.fromUndashed(ProtocolUtils.readString(buf, 32));
    }
    final String username = ProtocolUtils.readString(buf, 16);
    return new ClientboundServerLoginSuccessPacket(uuid, username);
  };

  private final UUID uuid;
  private final String username;

  public ClientboundServerLoginSuccessPacket(final UUID uuid, final String username) {
    this.uuid = Objects.requireNonNull(uuid, "uuid");
    this.username = Objects.requireNonNull(username, "username");
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_16)) {
      ProtocolUtils.writeUuidIntArray(buf, uuid);
    } else if (version.gte(ProtocolVersion.MINECRAFT_1_7_6)) {
      ProtocolUtils.writeString(buf, uuid.toString());
    } else {
      ProtocolUtils.writeString(buf, UuidUtils.toUndashed(uuid));
    }
    ProtocolUtils.writeString(buf, username);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public UUID getUuid() {
    return uuid;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("uuid", this.uuid)
      .add("username", this.username)
      .toString();
  }
}
