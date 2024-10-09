package src.Client;

import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

//Archivo con fallos, no reconoce las funciones en el cliente, posiblemente se elimine en el siguiente commit
public class EncryptionChat {

    // Texto fijo para derivar la clave secreta
    private static final String FIXED_TEXT = "TextoFijoParaClaveSecreta";

    // Clave secreta derivada de un texto fijo
    private static final SecretKey SECRET_KEY;

    static {
        try {
            // Derivar la clave secreta a partir del texto fijo
            byte[] salt = new byte[16]; // Puedes usar un salt fijo o aleatorio
            KeySpec spec = new PBEKeySpec(FIXED_TEXT.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] secretKeyBytes = factory.generateSecret(spec).getEncoded();
            SECRET_KEY = new SecretKeySpec(secretKeyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Error al derivar la clave secreta", e);
        }
    }

    // Método para obtener la clave secreta
    public static SecretKey getSecretKey() {
        return SECRET_KEY;
    }

    // Encriptar
    public static String encrypt(String message, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedMessage, SecretKey key) throws Exception {
        // Implementación del método de desencriptación usando la clave
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }
}
