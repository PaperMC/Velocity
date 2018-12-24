package com.velocitypowered.natives.encryption;

import java.security.GeneralSecurityException;

class MbedtlsAesImpl {

  native long init(byte[] key) throws GeneralSecurityException;

  native void process(long ctx, long sourceAddress, int sourceLength, long destinationAddress,
      boolean encrypt);

  native void free(long ptr);
}
