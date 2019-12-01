package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class MinecraftCipherDecoder extends MessageToMessageDecoder<ByteBuf> {

  private final VelocityCipher cipher;

  public MinecraftCipherDecoder(VelocityCipher cipher) {
    this.cipher = Preconditions.checkNotNull(cipher, "cipher");
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    ByteBuf compatible = MoreByteBufUtils.ensureCompatible(ctx.alloc(), cipher, in).slice();
    try {
      cipher.process(compatible);
      out.add(compatible);
      in.skipBytes(in.readableBytes());
    } catch (Exception e) {
      compatible.release(); // compatible will never be used if we throw an exception
      throw e;
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    cipher.dispose();
  }
}
