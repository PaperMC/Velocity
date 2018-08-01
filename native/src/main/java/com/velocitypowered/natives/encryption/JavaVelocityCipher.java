package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;

public class JavaVelocityCipher implements VelocityCipher {
    private final Cipher cipher;
    private boolean disposed = false;

    public JavaVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
        this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        this.cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
    }

    @Override
    public void process(ByteBuf source, ByteBuf destination) throws ShortBufferException {
        ensureNotDisposed();

        byte[] sourceAsBytes = new byte[source.readableBytes()];
        source.readBytes(sourceAsBytes);

        int outputSize = cipher.getOutputSize(sourceAsBytes.length);
        byte[] destinationBytes = new byte[outputSize];
        cipher.update(sourceAsBytes, 0, sourceAsBytes.length, destinationBytes);
        destination.writeBytes(destinationBytes);
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    private void ensureNotDisposed() {
        Preconditions.checkState(!disposed, "Object already disposed");
    }
}
