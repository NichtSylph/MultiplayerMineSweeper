package MPMineSweeper;

import java.security.*;
import javax.crypto.Cipher;

public class EncryptionUtil {
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public static byte[] decryptRSA(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    public static byte[] generateKeyExchangePayload(int playerNumber, long randomNumber, PublicKey publicKey) throws Exception {
        String keyBase = playerNumber + ":" + randomNumber;
        byte[] keyBytes = keyBase.getBytes();
        return encryptRSA(keyBytes, publicKey);
    }

    public static byte[] keyExchange(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        return decryptRSA(encryptedKey, privateKey);
    }

    public static byte[] xorEncryptDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }

        return result;
    }
}
