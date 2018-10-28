package com.velocitypowered.natives.encryption;

public class MbedtlsAesImpl {

  native long init(byte[] key);

  native void process(long ctx, long sourceAddress, int sourceLength, long destinationAddress,
      boolean encrypt);

  native void free(long ptr);
}
