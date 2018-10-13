package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public class EncryptionRequest implements MinecraftPacket {
    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    @Override
    public String toString() {
        return "EncryptionRequest{" +
                "publicKey=" + Arrays.toString(publicKey) +
                ", verifyToken=" + Arrays.toString(verifyToken) +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.serverId = ProtocolUtils.readString(buf, 20);
        publicKey = ProtocolUtils.readByteArray(buf, 256);
        verifyToken = ProtocolUtils.readByteArray(buf, 16);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, this.serverId);
        ProtocolUtils.writeByteArray(buf, publicKey);
        ProtocolUtils.writeByteArray(buf, verifyToken);
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
