package minesweeperjs;

public class EncryptionUtil {
    public static byte[] generateKey(int playerNumber, long randomNumber) {
        String keyBase = playerNumber + ":" + randomNumber;
        return keyBase.getBytes();
    }

    public static byte[] keyExchange(byte[] key1, byte[] key2) {
        byte[] result = new byte[key1.length];

        for (int i = 0; i < key1.length; i++) {
            result[i] = (byte) (key1[i] ^ key2[i]);
        }

        return result;
    }

    public static byte[] xorEncryptDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }

        return result;
    }
}
