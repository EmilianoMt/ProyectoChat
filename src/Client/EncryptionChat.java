package src.Client;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;



//Archivo con fallos, no reconoce las funciones en el cliente, posiblemente se elimine en el siguiente commit
public class EncryptionChat {

    // Genera una clave secreta AES
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES"); // Crea un generador de claves para AES
        keyGen.init(128); // Inicializa el generador para crear una clave de 128 bits
        return keyGen.generateKey(); // Genera y retorna la clave secreta
    }

   // Encriptar
    public static String encrypt(String data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); // IV estático (debería ser aleatorio para mayor seguridad)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Desencriptar
    public static String decrypt(String encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); // IV estático (debería ser el mismo usado en la encriptación)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
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
