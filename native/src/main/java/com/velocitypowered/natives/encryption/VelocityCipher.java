package com.velocitypowered.natives.encryption;

import com.velocitypowered.natives.Disposable;
import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;

public interface VelocityCipher extends Disposable, Native {
  void process(ByteBuf source);
}
