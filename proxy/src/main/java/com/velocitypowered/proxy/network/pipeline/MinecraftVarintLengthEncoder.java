package com.velocitypowered.proxy.network.pipeline;

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.network.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToByteEncoder<ByteBuf> {

  public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();
  private static final boolean IS_JAVA_CIPHER = Natives.cipher.get() == JavaVelocityCipher.FACTORY;

  private MinecraftVarintLengthEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    ProtocolUtils.writeVarInt(out, msg.readableBytes());
    out.writeBytes(msg);
  }

  @Override
  protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect)
      throws Exception {
    int anticipatedRequiredCapacity = 5 + msg.readableBytes();
    return IS_JAVA_CIPHER
        ? ctx.alloc().heapBuffer(anticipatedRequiredCapacity)
        : ctx.alloc().directBuffer(anticipatedRequiredCapacity);
  }
}
