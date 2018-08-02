package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public class EncryptionRequest implements MinecraftPacket {
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
        ProtocolUtils.readString(buf); // Server ID, can be ignored since it is an empty string
        publicKey = ProtocolUtils.readByteArray(buf, 256);
        verifyToken = ProtocolUtils.readByteArray(buf, 16);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeString(buf, ""); // Server ID
        ProtocolUtils.writeByteArray(buf, publicKey);
        ProtocolUtils.writeByteArray(buf, verifyToken);
    }
}
