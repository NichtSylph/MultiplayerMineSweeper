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

    public static void main(String[] args) throws Exception {

        // Client one
        KeyPair clientOneKeyPair = generateRSAKeyPair();
        PublicKey clientOnePublicKey = clientOneKeyPair.getPublic();
        PrivateKey clientOnePrivateKey = clientOneKeyPair.getPrivate();

        // Client two
        KeyPair clientTwoKeyPair = generateRSAKeyPair();
        PublicKey clientTwoPublicKey = clientTwoKeyPair.getPublic();
        PrivateKey clientTwoPrivateKey = clientTwoKeyPair.getPrivate();

        // Key exchange: client one sends her public key to client two, and vice versa
        byte[] clientOneKeyPayload = generateKeyExchangePayload(1, 123, clientTwoPublicKey);
        byte[] clientTwoKeyPayload = generateKeyExchangePayload(2, 456, clientOnePublicKey);
        // Send respective payloads here...

        // Client one receives client two's key exchange payload and decrypts it
        byte[] decryptedClientTwoKeyExchangePayload = keyExchange(clientTwoKeyPayload, clientOnePrivateKey);
        // Client one now has client two's key, eg. decryptedClientTwoKeyExchangePayload

        // Client two receives client one's key exchange payload and decrypts it
        byte[] decryptedClientOneExchangePayload = keyExchange(clientOneKeyPayload, clientTwoPrivateKey);
        // Client two now has Client one's key. eg. decryptedClientOneKeyExchangePayload

        // Client two and client one can use each other's public keys to encrypt messages

        // Client one wants to send a message to client two...
        String clientOneMessage = "Hello Client Two!";
        byte[] encryptedMessage = encryptRSA(clientOneMessage.getBytes(), clientTwoPublicKey);

        // Send stuff here

        // Client two receives the encrypted message and decrypts it with their private key
        byte[] decryptedMessage = decryptRSA(encryptedMessage, clientTwoPrivateKey);
        System.out.println("Decrypted message: " + new String(decryptedMessage));
    }
}
