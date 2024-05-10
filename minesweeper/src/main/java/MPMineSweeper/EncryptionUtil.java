package MPMineSweeper;

import java.security.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.KeyGenerator;


public class EncryptionUtil {
    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/ECB/PKCS5Padding";

        // Encrypt a string
        public static String encrypt(String plainText, String secretKey) {
            try {
                Key key = new SecretKeySpec(secretKey.getBytes(), AES_ALGORITHM);
                Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
                return Base64.getEncoder().encodeToString(encryptedBytes);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    
        // Decrypt a string
        public static String decrypt(String encryptedText, String secretKey) {
            try {
                Key key = new SecretKeySpec(secretKey.getBytes(), AES_ALGORITHM);
                Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, key);
                byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
                return new String(decryptedBytes);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

    public static String createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return java.util.Base64.getEncoder().encodeToString(keyGenerator.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

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
