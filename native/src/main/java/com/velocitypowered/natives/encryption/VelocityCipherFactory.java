package com.velocitypowered.natives.encryption;

import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;

public interface VelocityCipherFactory {

  VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException;

  VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException;
}
