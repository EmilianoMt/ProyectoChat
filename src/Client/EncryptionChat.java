package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EncryptionChat {

    // Generates a new secret key for encryption
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // 128-bit AES key
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating the encryption key", e);
        }
    }

    // Encrypts a message using the provided secret key
    public static String encrypt(String message, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes); // Return encrypted message in Base64 format
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting the message", e);
        }
    }

    // Decrypts a message using the provided secret key
    public static String decrypt(String encryptedMessage, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes); // Return decrypted message as a string
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting the message", e);
        }
    }
}
