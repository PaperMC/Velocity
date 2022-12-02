package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.Lists;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RemovePlayerInfo implements MinecraftPacket {
  private List<UUID> profilesToRemove;

  public RemovePlayerInfo() {
    this.profilesToRemove = new ArrayList<>();
  }

  public RemovePlayerInfo(List<UUID> profilesToRemove) {
    this.profilesToRemove = profilesToRemove;
  }

  public List<UUID> getProfilesToRemove() {
    return profilesToRemove;
  }

  public void setProfilesToRemove(List<UUID> profilesToRemove) {
    this.profilesToRemove = profilesToRemove;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int length = ProtocolUtils.readVarInt(buf);
    List<UUID> profilesToRemove = Lists.newArrayListWithCapacity(length);
    for (int idx = 0; idx < length; idx++) {
      profilesToRemove.add(ProtocolUtils.readUuid(buf));
    }
    this.profilesToRemove = profilesToRemove;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, this.profilesToRemove.size());
    for (UUID uuid : this.profilesToRemove) {
      ProtocolUtils.writeUuid(buf, uuid);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return false;
  }
}
