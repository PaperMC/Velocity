package com.velocitypowered.natives.encryption;

import com.velocitypowered.natives.Disposable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javax.crypto.ShortBufferException;

public interface VelocityCipher extends Disposable {

  void process(ByteBuf source, ByteBuf destination) throws ShortBufferException;

  ByteBuf process(ChannelHandlerContext ctx, ByteBuf source) throws ShortBufferException;
}
