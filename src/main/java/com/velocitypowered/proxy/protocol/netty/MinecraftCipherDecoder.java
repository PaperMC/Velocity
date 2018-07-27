package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.encryption.VelocityCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MinecraftCipherDecoder extends ByteToMessageDecoder {
    private final VelocityCipher cipher;

    public MinecraftCipherDecoder(VelocityCipher cipher) {
        this.cipher = Preconditions.checkNotNull(cipher, "cipher");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf decrypted = ctx.alloc().buffer();
        try {
            cipher.process(in, decrypted);
            out.add(decrypted);
        } catch (Exception e) {
            decrypted.release();
            throw e;
        }
    }
}
