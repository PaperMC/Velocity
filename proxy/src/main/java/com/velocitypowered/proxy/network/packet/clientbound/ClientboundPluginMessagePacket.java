package com.velocitypowered.proxy.network.packet.clientbound;

import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import io.netty.buffer.ByteBuf;

public class ClientboundPluginMessagePacket extends AbstractPluginMessagePacket<ClientboundPluginMessagePacket> implements Packet {
  public static final Factory<ClientboundPluginMessagePacket> FACTORY = ClientboundPluginMessagePacket::new;
  public static final PacketReader<ClientboundPluginMessagePacket> DECODER = decoder(FACTORY);

  public ClientboundPluginMessagePacket(final String channel, final ByteBuf backing) {
    super(channel, backing);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  @Override
  public ClientboundPluginMessagePacket replace(ByteBuf content) {
    return new ClientboundPluginMessagePacket(this.channel, content);
  }
}
