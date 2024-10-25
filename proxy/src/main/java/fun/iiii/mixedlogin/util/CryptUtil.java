package fun.iiii.mixedlogin.util;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CryptUtil {
    public static SecretKey createNewSharedKey()
    {
        try
        {
            KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
            keygenerator.init(128);
            return keygenerator.generateKey();
        }
        catch (NoSuchAlgorithmException nosuchalgorithmexception)
        {
            throw new Error(nosuchalgorithmexception);
        }
    }

    private static Cipher createTheCipherInstance(int opMode, String transformation, Key key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(opMode, key);
            return cipher;
        }
        catch (InvalidKeyException invalidkeyexception)
        {
            invalidkeyexception.printStackTrace();
        }
        catch (NoSuchAlgorithmException nosuchalgorithmexception)
        {
            nosuchalgorithmexception.printStackTrace();
        }
        catch (NoSuchPaddingException nosuchpaddingexception)
        {
            nosuchpaddingexception.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptData(Key key, byte[] data)
    {
        return cipherOperation(1, key, data);
    }

    private static byte[] cipherOperation(int opMode, Key key, byte[] data)
    {
        try
        {
            return createTheCipherInstance(opMode, key.getAlgorithm(), key).doFinal(data);
        }
        catch (IllegalBlockSizeException illegalblocksizeexception)
        {
            illegalblocksizeexception.printStackTrace();
        }
        catch (BadPaddingException badpaddingexception)
        {
            badpaddingexception.printStackTrace();
        }

        return null;
    }

    public static PublicKey decodePublicKey(byte[] encodedKey)
    {
        try
        {
            EncodedKeySpec encodedkeyspec = new X509EncodedKeySpec(encodedKey);
            KeyFactory keyfactory = KeyFactory.getInstance("RSA");
            return keyfactory.generatePublic(encodedkeyspec);
        }
        catch (NoSuchAlgorithmException var3)
        {
            ;
        }
        catch (InvalidKeySpecException var4)
        {
            ;
        }

        return null;
    }
    public static byte[] getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey)
    {
        try
        {
            return digestOperation("SHA-1", serverId.getBytes("ISO_8859_1"), secretKey.getEncoded(), publicKey.getEncoded());
        }
        catch (UnsupportedEncodingException unsupportedencodingexception)
        {
            unsupportedencodingexception.printStackTrace();
            return null;
        }
    }
    private static byte[] digestOperation(String algorithm, byte[]... data)
    {
        try
        {
            MessageDigest messagedigest = MessageDigest.getInstance(algorithm);

            for (byte[] abyte : data)
            {
                messagedigest.update(abyte);
            }

            return messagedigest.digest();
        }
        catch (NoSuchAlgorithmException nosuchalgorithmexception)
        {
            nosuchalgorithmexception.printStackTrace();
            return null;
        }
    }
}
