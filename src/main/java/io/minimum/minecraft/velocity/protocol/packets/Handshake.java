package io.minimum.minecraft.velocity.protocol.packets;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.ProtocolConstants;
import io.minimum.minecraft.velocity.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class Handshake implements MinecraftPacket {
    private int protocolVersion;
    private String serverAddress;
    private int port;
    private int nextStatus;

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
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
        return "Handshake{" +
                "protocolVersion=" + protocolVersion +
                ", serverAddress='" + serverAddress + '\'' +
                ", port=" + port +
                ", nextStatus=" + nextStatus +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        protocolVersion = ProtocolUtils.readVarInt(buf);
        serverAddress = ProtocolUtils.readString(buf, 255);
        port = buf.readUnsignedShort();
        nextStatus = ProtocolUtils.readVarInt(buf);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeVarInt(buf, protocolVersion);
        ProtocolUtils.writeString(buf, serverAddress);
        buf.writeShort(port);
        ProtocolUtils.writeVarInt(buf, nextStatus);
    }
}
