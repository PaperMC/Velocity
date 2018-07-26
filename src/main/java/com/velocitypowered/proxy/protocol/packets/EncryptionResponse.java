package com.velocitypowered.proxy.protocol.packets;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class EncryptionResponse implements MinecraftPacket {
    private byte[] sharedSecret;
    private byte[] verifyToken;

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.sharedSecret = ProtocolUtils.readByteArray(buf, 256);
        this.verifyToken = ProtocolUtils.readByteArray(buf, 4);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        ProtocolUtils.writeByteArray(buf, sharedSecret);
        ProtocolUtils.writeByteArray(buf, verifyToken);
    }
}
