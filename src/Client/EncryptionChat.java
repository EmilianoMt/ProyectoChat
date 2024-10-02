package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionChat {

    // Generate AES secret key
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // 128-bit key
        return keyGen.generateKey();
    }

    // Encrypt text with AES
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes); // Encode in base64 for easier transport
    }

    // Decrypt text with AES
    public static String decrypt(String encryptedText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    // Decode secret key from base64 string
    public static SecretKey decodeKey(String keyResponse) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(keyResponse);
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            // Manejar excepción de decodificación Base64
            System.err.println("Error: Clave Base64 no válida.");
            e.printStackTrace();
            return null; // O lanzar una excepción personalizada
        } catch (Exception e) {
            // Manejar cualquier otra excepción
            System.err.println("Error al decodificar la clave.");
            e.printStackTrace();
            return null; // O lanzar una excepción personalizada
        }
    }
}
