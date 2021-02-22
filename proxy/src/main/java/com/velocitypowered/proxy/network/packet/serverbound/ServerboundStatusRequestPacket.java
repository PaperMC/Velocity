package com.velocitypowered.proxy.network.packet.serverbound;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;

public class ServerboundStatusRequestPacket implements Packet {
  public static final ServerboundStatusRequestPacket INSTANCE = new ServerboundStatusRequestPacket();
  public static final PacketReader<ServerboundStatusRequestPacket> DECODER = PacketReader.instance(INSTANCE);
  public static final PacketWriter<ServerboundStatusRequestPacket> ENCODER = PacketWriter.noop();

  private ServerboundStatusRequestPacket() {
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "StatusRequestPacket";
  }
}
