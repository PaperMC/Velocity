package com.velocitypowered.natives.encryption;

import java.security.GeneralSecurityException;

class OpenSslCipherImpl {

  static native long init(byte[] key, boolean encrypt) throws GeneralSecurityException;

  static native void process(long ctx, long source, int len, long dest);

  static native void free(long ptr);
}
