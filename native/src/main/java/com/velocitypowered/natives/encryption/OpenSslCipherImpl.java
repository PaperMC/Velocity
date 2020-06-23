package com.velocitypowered.natives.encryption;

import java.security.GeneralSecurityException;

class OpenSslCipherImpl {

  native long init(byte[] key, boolean encrypt) throws GeneralSecurityException;

  native void process(long ctx, long source, int len, long dest);

  native void free(long ptr);
}
