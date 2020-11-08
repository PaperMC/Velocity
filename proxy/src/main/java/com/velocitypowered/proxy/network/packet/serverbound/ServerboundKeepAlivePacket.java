package com.velocitypowered.proxy.network.packet.serverbound;

import com.velocitypowered.proxy.network.packet.AbstractKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;

public class ServerboundKeepAlivePacket extends AbstractKeepAlivePacket implements Packet {
  public static final Decoder<ServerboundKeepAlivePacket> DECODER = decoder(ServerboundKeepAlivePacket::new);

  public ServerboundKeepAlivePacket(final long randomId) {
    super(randomId);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }
}
