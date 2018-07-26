package com.velocitypowered.proxy.util;

import java.math.BigInteger;

public enum EncryptionUtils { ;
    public static String twoComplementsSha1Digest(byte[] digest) {
        return new BigInteger(digest).toString(16);
    }
}
