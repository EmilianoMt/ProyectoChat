package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EncryptionChat {

    // Generar clave secreta AES
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);  // Clave de 128 bits
        return keyGen.generateKey();
    }

    // Encriptar texto con AES
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);  // Codificar en base64 para facilitar el transporte
    }

    // Desencriptar texto con AES
    public static String decrypt(String encryptedText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    // Decodificar clave secreta desde una cadena base64
    static SecretKey decodeKey(String keyResponse) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyResponse);
        return new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
