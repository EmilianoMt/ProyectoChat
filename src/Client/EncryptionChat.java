package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;



//Archivo con fallos, no reconoce las funciones en el cliente, posiblemente se elimine en el siguiente commit
public class EncryptionChat {

    // Genera una clave secreta AES
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES"); // Crea un generador de claves para AES
        keyGen.init(128); // Inicializa el generador para crear una clave de 128 bits
        return keyGen.generateKey(); // Genera y retorna la clave secreta
    }

    // Encripta texto con AES
    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES"); // Crea una instancia de Cipher para AES
        cipher.init(Cipher.ENCRYPT_MODE, secretKey); // Inicializa el Cipher en modo de encriptación con la clave secreta
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes()); // Encripta el texto plano
        return Base64.getEncoder().encodeToString(encryptedBytes); // Codifica en base64 para facilitar el transporte
    }

    // Desencripta texto con AES
    public static String decrypt(String encryptedText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES"); // Crea una instancia de Cipher para AES
        cipher.init(Cipher.DECRYPT_MODE, secretKey); // Inicializa el Cipher en modo de desencriptación con la clave secreta
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText); // Decodifica el texto encriptado de base64
        byte[] decryptedBytes = cipher.doFinal(decodedBytes); // Desencripta el texto
        return new String(decryptedBytes); // Convierte los bytes desencriptados a String y lo retorna
    }

    // Decodifica una clave secreta desde una cadena en base64
    public static SecretKey decodeKey(String encodedKey) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey); // Decodifica la clave en base64
            // Reconstruye la clave secreta a partir de los bytes decodificados
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Clave Base64 no válida."); // Imprime un mensaje de error si la clave base64 no es válida
            e.printStackTrace(); // Imprime la traza del error
            return null; // Retorna null en caso de error
        }
    }
}
