package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.encryption.VelocityCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MinecraftCipherEncoder extends MessageToByteEncoder<ByteBuf> {
    private final VelocityCipher cipher;

    public MinecraftCipherEncoder(VelocityCipher cipher) {
        this.cipher = Preconditions.checkNotNull(cipher, "cipher");
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        cipher.process(msg, out);
    }
}
