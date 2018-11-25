package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

public class TabCompleteResponse implements MinecraftPacket {

  private final List<String> offers = new ArrayList<>();

  public List<String> getOffers() {
    return offers;
  }

  @Override
  public String toString() {
    return "TabCompleteResponse{"
        + "offers=" + offers
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    int offersAvailable = ProtocolUtils.readVarInt(buf);
    for (int i = 0; i < offersAvailable; i++) {
      offers.add(ProtocolUtils.readString(buf));
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, offers.size());
    for (String offer : offers) {
      ProtocolUtils.writeString(buf, offer);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
