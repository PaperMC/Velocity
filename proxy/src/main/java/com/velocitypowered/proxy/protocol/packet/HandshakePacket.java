package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class HandshakePacket implements Packet {

  private ProtocolVersion protocolVersion;
  private String serverAddress = "";
  private int port;
  private int nextStatus;

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getNextStatus() {
    return nextStatus;
  }

  public void setNextStatus(int nextStatus) {
    this.nextStatus = nextStatus;
  }

  @Override
  public String toString() {
    return "Handshake{"
        + "protocolVersion=" + protocolVersion
        + ", serverAddress='" + serverAddress + '\''
        + ", port=" + port
        + ", nextStatus=" + nextStatus
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion ignored) {
    int realProtocolVersion = ProtocolUtils.readVarInt(buf);
    this.protocolVersion = ProtocolVersion.getProtocolVersion(realProtocolVersion);
    this.serverAddress = ProtocolUtils.readString(buf);
    this.port = buf.readUnsignedShort();
    this.nextStatus = ProtocolUtils.readVarInt(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion ignored) {
    ProtocolUtils.writeVarInt(buf, this.protocolVersion.getProtocol());
    ProtocolUtils.writeString(buf, this.serverAddress);
    buf.writeShort(this.port);
    ProtocolUtils.writeVarInt(buf, this.nextStatus);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
