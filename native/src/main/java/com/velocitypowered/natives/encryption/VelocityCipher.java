package com.velocitypowered.natives.encryption;

import com.velocitypowered.natives.Disposable;
import io.netty.buffer.ByteBuf;
import javax.crypto.ShortBufferException;

public interface VelocityCipher extends Disposable {

  void process(ByteBuf source, ByteBuf destination) throws ShortBufferException;
}
