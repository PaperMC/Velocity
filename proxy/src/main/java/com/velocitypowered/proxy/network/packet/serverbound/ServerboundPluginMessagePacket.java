package com.velocitypowered.proxy.network.packet.serverbound;

import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;

public class ServerboundPluginMessagePacket extends AbstractPluginMessagePacket<ServerboundPluginMessagePacket> implements Packet {
  public static final Factory<ServerboundPluginMessagePacket> FACTORY = ServerboundPluginMessagePacket::new;
  public static final PacketReader<ServerboundPluginMessagePacket> DECODER = decoder(FACTORY);
  public static final PacketWriter<ServerboundPluginMessagePacket> ENCODER = encoder();

  public ServerboundPluginMessagePacket(final String channel, final ByteBuf backing) {
    super(channel, backing);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  @Override
  public ServerboundPluginMessagePacket replace(ByteBuf content) {
    return new ServerboundPluginMessagePacket(this.channel, content);
  }
}
