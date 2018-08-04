package com.velocitypowered.natives.encryption;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

public interface VelocityCipherFactory {
    VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException;

    VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException;
}
