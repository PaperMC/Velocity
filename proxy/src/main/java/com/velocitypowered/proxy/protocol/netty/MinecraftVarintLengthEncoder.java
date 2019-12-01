package com.velocitypowered.proxy.protocol.netty;

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

@ChannelHandler.Sharable
public class MinecraftVarintLengthEncoder extends MessageToMessageEncoder<ByteBuf> {

  public static final MinecraftVarintLengthEncoder INSTANCE = new MinecraftVarintLengthEncoder();
  private static final boolean IS_JAVA_CIPHER = Natives.cipher.get() == JavaVelocityCipher.FACTORY;

  private MinecraftVarintLengthEncoder() {
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list)
      throws Exception {
    ByteBuf lengthBuf = IS_JAVA_CIPHER ? ctx.alloc().heapBuffer(5) : ctx.alloc().directBuffer(5);
    ProtocolUtils.writeVarInt(lengthBuf, buf.readableBytes());
    list.add(lengthBuf);
    list.add(buf.retain());
  }
}
